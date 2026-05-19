package com.supply.supplyTrace.config;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthCheck {

    @GetMapping("/")
    public Map<String, String> home() {
        return Map.of(
                "status", "UP",
                "service", "SupplyTrace",
                "message", "Supply chain monitoring backend is running"
        );
    }
}