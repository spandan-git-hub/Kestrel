package com.kestrel.alertfanout;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.kestrel.alertfanout", "com.kestrel.common"})
public class AlertFanoutApplication {

    public static void main(String[] args) {
        SpringApplication.run(AlertFanoutApplication.class, args);
    }
}
