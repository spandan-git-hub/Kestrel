package com.kestrel;

import static org.assertj.core.api.Assertions.assertThat;

import com.kestrel.repository.AlertStateRepository;
import com.kestrel.repository.LocationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@SpringBootTest
@AutoConfigureMockMvc
class KestrelApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private AlertStateRepository alertStateRepository;

    @Test
    void contextLoadsAndLocationsSeeded() {
        assertThat(locationRepository.count()).isEqualTo(4);
        assertThat(alertStateRepository.count()).isEqualTo(4);
    }

    @Test
    void getLocationsReturnsSeededData() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/locations")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(4))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].id").value("mumbai-kurla"))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].thresholds.twoYear").value(45.0));
    }
}
