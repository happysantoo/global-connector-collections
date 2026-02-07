package com.example.connector.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Demo application: receives messages via JMS, transforms through the connector pipeline,
 * and sends to Kafka. Demonstrates observability, resilience, replay, hold/release, health/control.
 */
@SpringBootApplication
public class DemoJmsKafkaApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoJmsKafkaApplication.class, args);
    }
}
