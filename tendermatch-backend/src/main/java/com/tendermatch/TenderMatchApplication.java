package com.tendermatch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class TenderMatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(TenderMatchApplication.class, args);
    }
}
