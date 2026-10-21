package com.shopper.pricing.infrastructure.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.shopper.pricing.application.service.PricingService;
import com.shopper.pricing.domain.repository.ComponentRepository;
import com.shopper.pricing.infrastructure.persistence.InMemoryComponentRepository;
import com.shopper.pricing.infrastructure.persistence.SeedData;

@Configuration
class PricingConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    ComponentRepository componentRepository() {
        var repository = new InMemoryComponentRepository();
        SeedData.loadComponents(repository);
        return repository;
    }

    @Bean
    PricingService pricingService(ComponentRepository components, Clock clock) {
        return new PricingService(components, SeedData.tariffs(), clock);
    }
}
