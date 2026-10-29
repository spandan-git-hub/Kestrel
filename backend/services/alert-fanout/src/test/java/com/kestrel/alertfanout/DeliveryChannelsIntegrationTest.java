package com.kestrel.alertfanout;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kestrel.alertfanout.api.dto.BroadcastPreviewRequest;
import com.kestrel.alertfanout.api.dto.BroadcastRequest;
import com.kestrel.alertfanout.infra.entity.DeliveryLog;
import com.kestrel.alertfanout.infra.repository.DeliveryLogRepository;
import com.kestrel.common.events.Channel;
import com.kestrel.common.events.Hazard;
import com.kestrel.common.events.Severity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DeliveryChannelsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DeliveryLogRepository deliveryLogRepository;

    @BeforeEach
    void setUp() {
        deliveryLogRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /api/v1/fanout/broadcast/preview returns multilingual text and WhatsApp share URL")
    void shouldGenerateBroadcastPreview() throws Exception {
        BroadcastPreviewRequest request = new BroadcastPreviewRequest(
                "mumbai-kurla",
                Hazard.FLOOD,
                Severity.WARNING,
                "Water level rising near Kurla railway station."
        );

        mockMvc.perform(post("/api/v1/fanout/broadcast/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locationId").value("mumbai-kurla"))
                .andExpect(jsonPath("$.locationName").value("Kurla Ward L, Mumbai"))
                .andExpect(jsonPath("$.severity").value("WARNING"))
                .andExpect(jsonPath("$.recipientCount").value(42))
                .andExpect(jsonPath("$.previews.en").exists())
                .andExpect(jsonPath("$.previews.hi").exists())
                .andExpect(jsonPath("$.previews.mr").exists())
                .andExpect(jsonPath("$.whatsAppShareUrl").value(org.hamcrest.Matchers.startsWith("https://wa.me/?text=")));
    }

    @Test
    @DisplayName("POST /api/v1/fanout/broadcast dispatches to real channels (Telegram & Web Push) and persists receipts in DynamoDB")
    void shouldExecuteBroadcastAndRecordInRecentQueue() throws Exception {
        BroadcastRequest request = new BroadcastRequest(
                "patna-ward12",
                Hazard.FLOOD,
                Severity.SEVERE,
                "High flood surge expected along Ganges bank.",
                List.of(Channel.TELEGRAM, Channel.WEB_PUSH)
        );

        mockMvc.perform(post("/api/v1/fanout/broadcast")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.broadcastId").exists())
                .andExpect(jsonPath("$.locationId").value("patna-ward12"))
                .andExpect(jsonPath("$.locationName").value("Ward 12, Patna"))
                .andExpect(jsonPath("$.severity").value("SEVERE"))
                .andExpect(jsonPath("$.dispatchedCount").value(greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.status").value("DISPATCHED"))
                .andExpect(jsonPath("$.whatsAppShareUrl").value(org.hamcrest.Matchers.startsWith("https://wa.me/?text=")));

        // Verify recent dispatches endpoint returns real DynamoDB entries
        mockMvc.perform(get("/api/v1/fanout/recent?limit=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$[0].locationId").value("patna-ward12"))
                .andExpect(jsonPath("$[0].severity").value("SEVERE"));

        // Verify database persistence contains real Telegram and Web Push logs
        List<DeliveryLog> logs = deliveryLogRepository.findByLocationIdOrderByTimestampDesc("patna-ward12");
        assertThat(logs).isNotEmpty();
        assertThat(logs.get(0).getSeverity()).isEqualTo(Severity.SEVERE);
        assertThat(logs).anySatisfy(l -> assertThat(l.getChannel()).isEqualTo(Channel.TELEGRAM));
        assertThat(logs).anySatisfy(l -> assertThat(l.getChannel()).isEqualTo(Channel.WEB_PUSH));
    }
}
