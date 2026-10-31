package com.urlshortener.service;

import com.urlshortener.model.Url;
import com.urlshortener.model.User;
import com.urlshortener.repository.UrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies the Spring Cache behaviour on UrlService without needing Redis or a database:
 * a lightweight in-memory CacheManager stands in for Redis, the repository is mocked.
 */
@SpringJUnitConfig(UrlCachingTest.TestConfig.class)
class UrlCachingTest {

    @Configuration
    @EnableCaching
    static class TestConfig {
        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("urls");
        }

        @Bean
        UrlRepository urlRepository() {
            return Mockito.mock(UrlRepository.class);
        }

        @Bean
        UrlService urlService(UrlRepository urlRepository) {
            return new UrlService(urlRepository);
        }
    }

    @Autowired
    private UrlService urlService;
    @Autowired
    private UrlRepository urlRepository;
    @Autowired
    private CacheManager cacheManager;

    private final User user = User.builder()
            .id(1L).name("jane").email("jane@example.com")
            .password("hash").role("ROLE_USER").build();

    @BeforeEach
    void reset() {
        Mockito.reset(urlRepository);
        cacheManager.getCache("urls").clear();
    }

    private Url url(String code, String longUrl) {
        Url url = new Url();
        url.setShortCode(code);
        url.setLongUrl(longUrl);
        return url;
    }

    @Test
    void repeatedLookups_hitTheDatabaseOnlyOnce() {
        when(urlRepository.findByShortCode("abc"))
                .thenReturn(Optional.of(url("abc", "https://example.com")));

        assertEquals("https://example.com", urlService.getLongUrl("abc"));
        assertEquals("https://example.com", urlService.getLongUrl("abc"));
        assertEquals("https://example.com", urlService.getLongUrl("abc"));

        verify(urlRepository, times(1)).findByShortCode("abc");
    }

    @Test
    void deletingAUrl_evictsItFromTheCache() {
        Url stored = url("abc", "https://example.com");
        when(urlRepository.findByShortCode("abc")).thenReturn(Optional.of(stored));
        when(urlRepository.findByShortCodeAndUser("abc", user)).thenReturn(Optional.of(stored));

        assertEquals("https://example.com", urlService.getLongUrl("abc")); // now cached

        urlService.deleteUserUrl("abc", user);

        // after deletion the database no longer has the code
        when(urlRepository.findByShortCode("abc")).thenReturn(Optional.empty());
        assertNull(urlService.getLongUrl("abc"),
                "a deleted short URL must stop redirecting immediately");
    }

    @Test
    void unknownCodes_areNotCached() {
        when(urlRepository.findByShortCode("fresh")).thenReturn(Optional.empty());
        assertNull(urlService.getLongUrl("fresh"));

        // the code is created afterwards - it must be found, not hidden by a cached "null"
        when(urlRepository.findByShortCode("fresh"))
                .thenReturn(Optional.of(url("fresh", "https://example.com")));
        assertEquals("https://example.com", urlService.getLongUrl("fresh"));
    }
}
