package com.locationapp.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Main Spring Boot Application
 *
 * This is like the Application class in Android that initializes the app.
 *
 * @SpringBootApplication combines:
 * - @Configuration: Marks this as a configuration class
 * - @EnableAutoConfiguration: Auto-configures Spring Boot features
 * - @ComponentScan: Scans for @Component, @Service, @Controller, etc.
 */
@SpringBootApplication
@EnableCaching      // Enable Redis caching
@EnableAsync        // Enable async method execution
@EnableJpaAuditing  // Enable automatic timestamp fields (@CreatedDate, @LastModifiedDate)
public class LocationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LocationServiceApplication.class, args);
    }
}
