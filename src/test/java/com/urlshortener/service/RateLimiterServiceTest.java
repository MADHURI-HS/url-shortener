package com.urlshortener.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimiterServiceTest {

    private final RateLimiterService service = new RateLimiterService();

    @Test
    void allowsFiveRequestsThenBlocks() {
        for (int i = 0; i < 5; i++) {
            assertTrue(service.tryConsume("1.1.1.1"), "request " + (i + 1) + " should pass");
        }
        assertFalse(service.tryConsume("1.1.1.1"), "6th request in a minute must be blocked");
    }

    @Test
    void limitsAreTrackedPerIp() {
        for (int i = 0; i < 5; i++) {
            service.tryConsume("1.1.1.1");
        }
        assertFalse(service.tryConsume("1.1.1.1"));
        assertTrue(service.tryConsume("2.2.2.2"), "a different IP has its own bucket");
    }
}
