package com.kestrel.subscriberregistry.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kestrel.common.events.EventObjectMapperFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class RestClientConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        return EventObjectMapperFactory.createObjectMapper();
    }

    @Bean("eventBusRestClient")
    public RestClient eventBusRestClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(3000));
        requestFactory.setReadTimeout(Duration.ofMillis(5000));

        return RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }
}
