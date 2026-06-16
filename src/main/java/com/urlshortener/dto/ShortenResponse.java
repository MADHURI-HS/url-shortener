package com.urlshortener.dto;

public class ShortenResponse {
    private String shortUrl;
    private String longUrl;

    public ShortenResponse(String shortUrl, String longUrl) {
        this.shortUrl = shortUrl;
        this.longUrl = longUrl;
    }

    public String getShortUrl() { return shortUrl; }
    public String getLongUrl() { return longUrl; }
}