package com.kestrel.subscriberregistry.api;

import com.kestrel.subscriberregistry.api.dto.CreateSubscriptionRequest;
import com.kestrel.subscriberregistry.api.dto.CreateSubscriptionResponse;
import com.kestrel.subscriberregistry.api.dto.SubscriptionResponse;
import com.kestrel.subscriberregistry.app.RegistryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/registry/subscriptions")
public class SubscriptionController {

    private final RegistryService registryService;

    public SubscriptionController(RegistryService registryService) {
        this.registryService = registryService;
    }

    @PostMapping
    public ResponseEntity<CreateSubscriptionResponse> createSubscription(
            @Valid @RequestBody CreateSubscriptionRequest request) {
        CreateSubscriptionResponse response = registryService.createSubscription(request);
        URI locationUri = URI.create("/api/v1/registry/subscriptions/" + response.token());
        return ResponseEntity.created(locationUri).body(response);
    }

    @GetMapping("/{token}")
    public ResponseEntity<SubscriptionResponse> getSubscription(@PathVariable String token) {
        return ResponseEntity.ok(registryService.getSubscription(token));
    }

    @GetMapping
    public ResponseEntity<List<SubscriptionResponse>> getSubscriptions(
            @RequestParam(required = false) String locationId) {
        return ResponseEntity.ok(registryService.getSubscriptions(locationId));
    }

    @DeleteMapping("/{token}")
    public ResponseEntity<Void> deleteSubscription(@PathVariable String token) {
        registryService.deleteSubscription(token);
        return ResponseEntity.noContent().build();
    }
}
