package com.urlshortener.service;

import com.urlshortener.model.Url;
import com.urlshortener.repository.UrlRepository;
import com.urlshortener.util.Base62Encoder;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class UrlService {

    private final UrlRepository urlRepository;
    private final AtomicLong counter;

    public UrlService(UrlRepository urlRepository) {
        this.urlRepository = urlRepository;
        long lastId = urlRepository.findTopByOrderByIdDesc()
                .map(Url::getId)
                .orElse(99999L);
        this.counter = new AtomicLong(lastId + 1);
    }

    public String shortenUrl(String longUrl) {
        return urlRepository.findByLongUrl(longUrl)
                .map(Url::getShortCode)
                .orElseGet(() -> {
                    String code = Base62Encoder.encode(counter.getAndIncrement());
                    Url url = new Url();
                    url.setShortCode(code);
                    url.setLongUrl(longUrl);
                    urlRepository.save(url);
                    return code;
                });
    }

    @Cacheable(value = "urls", key = "#code")  // ← cache on read
    public String getLongUrl(String code) {
        System.out.println("CACHE MISS — hitting PostgreSQL for: " + code);
        return urlRepository.findByShortCode(code)
                .map(Url::getLongUrl)
                .orElse(null);
    }

    @CacheEvict(value = "urls", key = "#code")  // ← clear cache if URL deleted
    public void deleteUrl(String code) {
        urlRepository.findByShortCode(code)
                .ifPresent(urlRepository::delete);
    }
}