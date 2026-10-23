package com.kestrel.rulesengine.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kestrel.common.events.EventObjectMapperFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class RestClientConfig {

    @Value("${kestrel.flood-ingest.base-url:http://localhost:8082}")
    private String floodIngestBaseUrl;

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        return EventObjectMapperFactory.createObjectMapper();
    }

    @Bean
    public RestClient eventBusRestClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(3000));
        requestFactory.setReadTimeout(Duration.ofMillis(5000));

        return RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    @Bean
    public RestClient floodIngestRestClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(5000));
        requestFactory.setReadTimeout(Duration.ofMillis(10000));

        return RestClient.builder()
                .baseUrl(floodIngestBaseUrl)
                .requestFactory(requestFactory)
                .build();
    }
}
