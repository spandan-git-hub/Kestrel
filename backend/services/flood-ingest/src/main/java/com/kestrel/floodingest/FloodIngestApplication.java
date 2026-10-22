package com.kestrel.floodingest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.kestrel.floodingest", "com.kestrel.common"})
public class FloodIngestApplication {

    public static void main(String[] args) {
        SpringApplication.run(FloodIngestApplication.class, args);
    }
}
