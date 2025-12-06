package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.logrhythm")
public class LogRhythmApplication {

    public static void main(String[] args) {
        SpringApplication.run(LogRhythmApplication.class, args);
    }
}
