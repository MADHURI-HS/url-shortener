package com.urlshortener.dto;

import java.time.LocalDateTime;

public record UrlResponse(
        String shortCode,
        String longUrl,
        LocalDateTime createdAt
) {
}