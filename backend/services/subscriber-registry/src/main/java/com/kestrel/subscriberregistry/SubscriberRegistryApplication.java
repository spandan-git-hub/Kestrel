package com.kestrel.subscriberregistry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.kestrel.subscriberregistry", "com.kestrel.common"})
public class SubscriberRegistryApplication {

    public static void main(String[] args) {
        SpringApplication.run(SubscriberRegistryApplication.class, args);
    }
}
