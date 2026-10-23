package com.kestrel.alertfanout;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kestrel.alertfanout.infra.entity.LocationReplica;
import com.kestrel.alertfanout.infra.repository.LocationReplicaRepository;
import com.kestrel.common.events.EventEnvelope;
import com.kestrel.common.events.Language;
import com.kestrel.common.events.LocationUpsertedPayload;
import com.kestrel.common.events.Provenance;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class LocationReplicationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LocationReplicaRepository replicaRepository;

    @Test
    @DisplayName("Pilot locations are seeded on alert-fanout startup")
    void shouldHaveSeededPilotLocations() {
        assertThat(replicaRepository.count()).isGreaterThanOrEqualTo(4);
        Optional<LocationReplica> mumbai = replicaRepository.findById("mumbai-kurla");
        assertThat(mumbai).isPresent();
        assertThat(mumbai.get().getName()).isEqualTo("Kurla Ward L, Mumbai");
        assertThat(mumbai.get().getRiverName()).isEqualTo("Mithi River");
        assertThat(mumbai.get().getPrimaryLanguage()).isEqualTo(Language.mr);
        assertThat(mumbai.get().getCurrentProvenance()).isEqualTo(Provenance.MODEL_ONLY);
    }

    @Test
    @DisplayName("POST /api/v1/fanout/events/location-upserted replicates location into isolated DB")
    void shouldReplicateLocationUpsertedEvent() throws Exception {
        LocationUpsertedPayload payload = new LocationUpsertedPayload(
                "delhi-yamuna",
                "Yamuna Bridge, Delhi",
                "Delhi",
                28.6667,
                77.2167,
                "Yamuna River",
                205.33,
                206.50,
                208.00,
                Language.hi,
                true
        );
        EventEnvelope<LocationUpsertedPayload> envelope = EventEnvelope.of(
                "LocationUpserted",
                "subscriber-registry",
                "corr-test-fanout-01",
                payload
        );

        mockMvc.perform(post("/api/v1/fanout/events/location-upserted")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(envelope)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REPLICATED"))
                .andExpect(jsonPath("$.locationId").value("delhi-yamuna"));

        Optional<LocationReplica> replicaOpt = replicaRepository.findById("delhi-yamuna");
        assertThat(replicaOpt).isPresent();
        LocationReplica replica = replicaOpt.get();
        assertThat(replica.getName()).isEqualTo("Yamuna Bridge, Delhi");
        assertThat(replica.getState()).isEqualTo("Delhi");
        assertThat(replica.getRiverName()).isEqualTo("Yamuna River");
        assertThat(replica.getThreshold2yr()).isEqualTo(205.33);
        assertThat(replica.getPrimaryLanguage()).isEqualTo(Language.hi);
        assertThat(replica.getCurrentProvenance()).isEqualTo(Provenance.MODEL_ONLY);
    }
}
