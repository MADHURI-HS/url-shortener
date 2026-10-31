package com.urlshortener.service;

import com.urlshortener.dto.UrlResponse;
import com.urlshortener.exception.UrlNotFoundException;
import com.urlshortener.model.Url;
import com.urlshortener.model.User;
import com.urlshortener.repository.UrlRepository;
import com.urlshortener.util.Base62Encoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class UrlService {
    private static final Logger log = LoggerFactory.getLogger(UrlService.class);

    /** Added to the database id so the first short codes are already 3+ characters long. */
    static final long CODE_OFFSET = 100_000L;

    private final UrlRepository urlRepository;

    public UrlService(UrlRepository urlRepository) {
        this.urlRepository = urlRepository;
    }

    /**
     * Returns the existing short code for this user and URL, or creates a new one.
     * The code is Base62(CODE_OFFSET + database id). The database assigns the id, so codes
     * are unique across restarts and across multiple application instances.
     */
    @Transactional
    public String shortenUrl(String longUrl, User user) {

        return urlRepository
                .findByLongUrlAndUser(longUrl, user)
                .map(Url::getShortCode)
                .orElseGet(() -> createShortUrl(longUrl, user));
    }

    private String createShortUrl(String longUrl, User user) {

        Url url = new Url();
        url.setLongUrl(longUrl);
        url.setUser(user);
        // short_code is NOT NULL + UNIQUE, so insert with a unique placeholder first
        // to obtain the generated id, then replace it with the real code.
        url.setShortCode(UUID.randomUUID().toString().replace("-", ""));

        Url saved = urlRepository.saveAndFlush(url);

        String code = Base62Encoder.encode(CODE_OFFSET + saved.getId());
        saved.setShortCode(code);
        urlRepository.save(saved);

        return code;
    }

    // unless: never cache "not found", so a code created later is not hidden for 24h
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

    // Evicts the cache entry so a deleted URL stops redirecting immediately.
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
