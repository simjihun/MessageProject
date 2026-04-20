package com.messaging;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MessagingPlatformApp {
    public static void main(String[] args) {
        SpringApplication.run(MessagingPlatformApp.class, args);
    }
}
