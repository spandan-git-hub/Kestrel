package com.kestrel.common.core.filter;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CorrelationIdFilterTest {

    private CorrelationIdFilter filter;

    @BeforeEach
    void setUp() {
        filter = new CorrelationIdFilter();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void testFilterGeneratesCorrelationIdWhenMissing() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        String correlationId = response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
        assertThat(correlationId).isNotNull().isNotBlank();
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull(); // Cleared in finally
    }

    @Test
    void testFilterPreservesIncomingCorrelationId() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        String incomingId = "custom-client-id-999";
        request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, incomingId);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        String correlationId = response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
        assertThat(correlationId).isEqualTo(incomingId);
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull(); // Cleared in finally
    }
}
