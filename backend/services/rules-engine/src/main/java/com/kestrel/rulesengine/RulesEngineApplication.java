package com.kestrel.rulesengine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.kestrel.rulesengine", "com.kestrel.common"})
public class RulesEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(RulesEngineApplication.class, args);
    }
}
