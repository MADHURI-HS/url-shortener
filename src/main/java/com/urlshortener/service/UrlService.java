package com.urlshortener.service;

import com.urlshortener.dto.UrlResponse;
import com.urlshortener.exception.UrlNotFoundException;
import com.urlshortener.model.Url;
import com.urlshortener.model.User;
import com.urlshortener.repository.UrlRepository;
import com.urlshortener.util.Base62Encoder;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class UrlService {
    private static final Logger log = LoggerFactory.getLogger(UrlService.class);

    private final UrlRepository urlRepository;
    private final AtomicLong counter;

    public UrlService(UrlRepository urlRepository) {

        this.urlRepository = urlRepository;

        long lastId = urlRepository.findTopByOrderByIdDesc()
                .map(Url::getId)
                .orElse(99999L);

        this.counter = new AtomicLong(lastId + 1);
    }

    public String shortenUrl(String longUrl, User user) {

        return urlRepository
                .findByLongUrlAndUser(longUrl, user)
                .map(Url::getShortCode)
                .orElseGet(() -> {

                    String code =
                            Base62Encoder.encode(counter.getAndIncrement());

                    Url url = new Url();

                    url.setShortCode(code);
                    url.setLongUrl(longUrl);
                    url.setUser(user);

                    urlRepository.save(url);

                    return code;
                });
    }

    // unless: never cache "not found" so a code created later is not hidden for 24h
    @Cacheable(value = "urls", key = "#code", unless = "#result == null")
    public String getLongUrl(String code) {

        log.info("Cache miss. Fetching from database.");

        return urlRepository.findByShortCode(code)
                .map(Url::getLongUrl)
                .orElse(null);
    }

    public List<UrlResponse> getUrlsByUser(User user) {

        return urlRepository.findByUser(user)
                .stream()
                .map(url -> new UrlResponse(
                        url.getShortCode(),
                        url.getLongUrl(),
                        url.getCreatedAt()
                ))
                .toList();
    }

    // FIX: evict the Redis entry so a deleted URL stops redirecting immediately
    // (previously the cached value kept working for up to 24 hours).
    @CacheEvict(value = "urls", key = "#shortCode")
    @Transactional
    public void deleteUserUrl(String shortCode, User user) {

        Url url = urlRepository.findByShortCodeAndUser(shortCode, user)
                .orElseThrow(() ->
                        new UrlNotFoundException("URL not found"));

        urlRepository.delete(url);

        log.info("Deleted URL {}", shortCode);
    }
}
