package com.kestrel.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiGatewayApplicationTests {

    @Autowired
    private RouteLocator routeLocator;

    @Test
    void contextLoads() {
        assertThat(routeLocator).isNotNull();
    }

    @Test
    void expectedRoutesAreConfigured() {
        List<Route> routes = routeLocator.getRoutes().collectList().block();
        assertThat(routes).isNotNull();

        List<String> routeIds = routes.stream().map(Route::getId).toList();
        assertThat(routeIds).contains(
                "subscriber-registry-route",
                "flood-ingest-route",
                "rules-engine-route",
                "alert-fanout-route"
        );

        Route registryRoute = routes.stream()
                .filter(r -> "subscriber-registry-route".equals(r.getId()))
                .findFirst()
                .orElseThrow();
        assertThat(registryRoute.getUri().toString()).isEqualTo("http://localhost:8081");

        Route floodRoute = routes.stream()
                .filter(r -> "flood-ingest-route".equals(r.getId()))
                .findFirst()
                .orElseThrow();
        assertThat(floodRoute.getUri().toString()).isEqualTo("http://localhost:8082");

        Route rulesRoute = routes.stream()
                .filter(r -> "rules-engine-route".equals(r.getId()))
                .findFirst()
                .orElseThrow();
        assertThat(rulesRoute.getUri().toString()).isEqualTo("http://localhost:8083");

        Route fanoutRoute = routes.stream()
                .filter(r -> "alert-fanout-route".equals(r.getId()))
                .findFirst()
                .orElseThrow();
        assertThat(fanoutRoute.getUri().toString()).isEqualTo("http://localhost:8084");
    }
}
