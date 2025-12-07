package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.logarythm")
public class LogRhythmApplication {

    public static void main(String[] args) {
        SpringApplication.run(LogRhythmApplication.class, args);
    }
}
