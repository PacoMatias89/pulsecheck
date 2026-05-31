package com.pulsecheck;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PulseCheckApplication {
    public static void main(String[] args) {
        SpringApplication.run(PulseCheckApplication.class, args);
    }
}
