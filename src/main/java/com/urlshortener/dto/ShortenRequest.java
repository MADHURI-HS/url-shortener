package com.urlshortener.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShortenRequest {

    @NotBlank(message = "URL cannot be empty")
    @Schema(
            description = "Long URL to shorten",
            example = "https://www.google.com"
    )
    private String longUrl;
}