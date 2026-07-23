package com.urlshortener.controller;

import com.urlshortener.dto.ApiResponse;
import com.urlshortener.dto.ShortenRequest;
import com.urlshortener.dto.ShortenResponse;
import com.urlshortener.dto.UrlResponse;
import com.urlshortener.model.User;
import com.urlshortener.service.RateLimiterService;
import com.urlshortener.service.UrlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(
        name = "URL Management",
        description = "Create and manage shortened URLs"
)
@RestController
@RequestMapping("/api")
public class UrlController {

    private final UrlService urlService;
    private final RateLimiterService rateLimiterService;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    public UrlController(
            UrlService urlService,
            RateLimiterService rateLimiterService) {

        this.urlService = urlService;
        this.rateLimiterService = rateLimiterService;
    }

    @Operation(
            summary = "Shorten a URL",
            description = "Creates a unique short URL for the authenticated user."
    )
    @PostMapping("/shorten")
    public ResponseEntity<?> shortenUrl(
            @Valid @RequestBody ShortenRequest request,
            HttpServletRequest httpRequest,
            @AuthenticationPrincipal User user) {

        String clientIp = httpRequest.getHeader("X-Forwarded-For");

        if (clientIp == null || clientIp.isBlank()) {
            clientIp = httpRequest.getRemoteAddr();
        }

        if (!rateLimiterService.tryConsume(clientIp)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of(
                            "error",
                            "Too many requests. Try again in 1 minute."
                    ));
        }

        String code = urlService.shortenUrl(request.getLongUrl(), user);

        String shortUrl = baseUrl + "/" + code;

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Short URL created successfully",
                        new ShortenResponse(
                                shortUrl,
                                request.getLongUrl()
                        )
                )
        );
    }

    @Operation(
            summary = "Get My URLs",
            description = "Returns all URLs created by the authenticated user."
    )
    @GetMapping("/urls")
    public ResponseEntity<ApiResponse<List<UrlResponse>>> getUrls(
            @AuthenticationPrincipal User user) {

        List<UrlResponse> urls = urlService.getUrlsByUser(user);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "URLs fetched successfully",
                        urls
                )
        );
    }

    @Operation(
            summary = "Delete URL",
            description = "Deletes a URL created by the authenticated user."
    )
    @DeleteMapping("/urls/{shortCode}")
    public ResponseEntity<ApiResponse<Void>> deleteUrl(
            @PathVariable String shortCode,
            @AuthenticationPrincipal User user) {

        urlService.deleteUserUrl(shortCode, user);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "URL deleted successfully",
                        null
                )
        );
    }
}