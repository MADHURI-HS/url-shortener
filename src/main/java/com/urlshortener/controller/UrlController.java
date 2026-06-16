package com.urlshortener.controller;

import com.urlshortener.dto.ShortenRequest;
import com.urlshortener.dto.ShortenResponse;
import com.urlshortener.service.RateLimiterService;
import com.urlshortener.service.UrlService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class UrlController {

    private final UrlService urlService;
    private final RateLimiterService rateLimiterService;

    public UrlController(UrlService urlService, RateLimiterService rateLimiterService) {
        this.urlService = urlService;
        this.rateLimiterService = rateLimiterService;
    }

    @PostMapping("/shorten")
    public ResponseEntity<?> shortenUrl(
            @Valid @RequestBody ShortenRequest request,
            HttpServletRequest httpRequest) {

        // Get client IP
        String clientIp = httpRequest.getRemoteAddr();

        // Check rate limit
        if (!rateLimiterService.tryConsume(clientIp)) {
            return ResponseEntity
                    .status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "Too many requests. Try again in 1 minute."));
        }

        String code = urlService.shortenUrl(request.getLongUrl());
        String shortUrl = "http://localhost:8080/" + code;
        return ResponseEntity.ok(new ShortenResponse(shortUrl, request.getLongUrl()));
    }

    @GetMapping("/ping")
    public String ping() { return "pong"; }

    @GetMapping("/{code}")
    public ResponseEntity<Void> redirect(@PathVariable String code) {
        String longUrl = urlService.getLongUrl(code);
        if (longUrl == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity
                .status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, longUrl)
                .build();
    }
}