package com.kestrel.aqingest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.kestrel.aqingest", "com.kestrel.common.core"})
public class AqIngestApplication {

    public static void main(String[] args) {
        SpringApplication.run(AqIngestApplication.class, args);
    }
}

