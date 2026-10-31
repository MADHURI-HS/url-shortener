package com.urlshortener.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShortenRequest {

    @NotBlank(message = "URL cannot be empty")
    @Size(max = 2048, message = "URL must be at most 2048 characters")
    @Pattern(regexp = "^https?://\\S+$",
            flags = Pattern.Flag.CASE_INSENSITIVE,
            message = "URL must start with http:// or https://")
    @Schema(
            description = "Long URL to shorten",
            example = "https://www.google.com"
    )
    private String longUrl;
}