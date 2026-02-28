package com.piggy.piggybackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class PiggyBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(PiggyBackendApplication.class, args);
    }
}
