package com.kestrel.aqingest.config;

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

    @Value("${kestrel.open-meteo.aq-url:https://air-quality-api.open-meteo.com/v1/air-quality}")
    private String openMeteoAqUrl;

    @Value("${kestrel.open-meteo.connect-timeout-ms:5000}")
    private int connectTimeoutMs;

    @Value("${kestrel.open-meteo.read-timeout-ms:10000}")
    private int readTimeoutMs;

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        return EventObjectMapperFactory.createObjectMapper();
    }

    @Bean("openMeteoAqRestClient")
    public RestClient openMeteoAqRestClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));

        return RestClient.builder()
                .baseUrl(openMeteoAqUrl)
                .requestFactory(requestFactory)
                .build();
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

