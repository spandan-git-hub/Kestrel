package com.kestrel.heatingest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
        "com.kestrel.heatingest",
        "com.kestrel.common.core"
})
public class HeatIngestApplication {

    public static void main(String[] args) {
        SpringApplication.run(HeatIngestApplication.class, args);
    }
}

