package com.kestrel.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.kestrel.gateway.filter.CorrelationIdGlobalFilter;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

class CorrelationIdGlobalFilterTest {

    private final CorrelationIdGlobalFilter filter = new CorrelationIdGlobalFilter();

    @Test
    void generatesCorrelationIdWhenMissing() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/rules/status").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicReference<String> passedCorrelationId = new AtomicReference<>();
        GatewayFilterChain chain = mutatedExchange -> {
            passedCorrelationId.set(mutatedExchange.getRequest().getHeaders().getFirst(CorrelationIdGlobalFilter.CORRELATION_ID_HEADER));
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertThat(passedCorrelationId.get()).isNotNull().isNotBlank();
        assertThat(exchange.getResponse().getHeaders().getFirst(CorrelationIdGlobalFilter.CORRELATION_ID_HEADER))
                .isEqualTo(passedCorrelationId.get());
    }

    @Test
    void preservesExistingCorrelationId() {
        String existingId = "custom-correlation-12345";
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/flood/simulate")
                .header(CorrelationIdGlobalFilter.CORRELATION_ID_HEADER, existingId)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicReference<String> passedCorrelationId = new AtomicReference<>();
        GatewayFilterChain chain = mutatedExchange -> {
            passedCorrelationId.set(mutatedExchange.getRequest().getHeaders().getFirst(CorrelationIdGlobalFilter.CORRELATION_ID_HEADER));
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertThat(passedCorrelationId.get()).isEqualTo(existingId);
        assertThat(exchange.getResponse().getHeaders().getFirst(CorrelationIdGlobalFilter.CORRELATION_ID_HEADER))
                .isEqualTo(existingId);
    }
}
