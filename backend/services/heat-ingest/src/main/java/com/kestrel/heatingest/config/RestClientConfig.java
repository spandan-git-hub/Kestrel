package com.kestrel.heatingest.config;

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

    @Value("${kestrel.open-meteo.weather-url:https://api.open-meteo.com/v1/forecast}")
    private String openMeteoWeatherUrl;

    @Value("${kestrel.open-meteo.connect-timeout-ms:5000}")
    private int connectTimeoutMs;

    @Value("${kestrel.open-meteo.read-timeout-ms:10000}")
    private int readTimeoutMs;

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        return EventObjectMapperFactory.createObjectMapper();
    }

    @Bean("openMeteoWeatherRestClient")
    public RestClient openMeteoWeatherRestClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));

        return RestClient.builder()
                .baseUrl(openMeteoWeatherUrl)
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

