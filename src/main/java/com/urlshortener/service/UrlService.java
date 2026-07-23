package com.urlshortener.service;

import com.urlshortener.dto.UrlResponse;
import com.urlshortener.model.Url;
import com.urlshortener.model.User;
import com.urlshortener.repository.UrlRepository;
import com.urlshortener.util.Base62Encoder;
import jakarta.transaction.Transactional;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import jakarta.transaction.Transactional;

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

    @Cacheable(value = "urls", key = "#code")
    public String getLongUrl(String code) {

        log.info("Cache miss. Fetching from PostgreSQL.");

        return urlRepository.findByShortCode(code)
                .map(Url::getLongUrl)
                .orElse(null);
    }

    @CacheEvict(value = "urls", key = "#code")
    public void deleteUrl(String code) {

        urlRepository.findByShortCode(code)
                .ifPresent(urlRepository::delete);
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



    @Transactional
    public void deleteUserUrl(String shortCode, User user) {

        Url url = urlRepository.findByShortCodeAndUser(shortCode, user)
                .orElseThrow(() ->
                        new RuntimeException("URL not found"));

        urlRepository.delete(url);

        log.info("Deleted URL {}", shortCode);
    }
}