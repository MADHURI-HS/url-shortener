package com.urlshortener.service;

import com.urlshortener.dto.UrlResponse;
import com.urlshortener.exception.UrlNotFoundException;
import com.urlshortener.model.Url;
import com.urlshortener.model.User;
import com.urlshortener.repository.UrlRepository;
import com.urlshortener.util.Base62Encoder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UrlServiceTest {

    @Mock
    private UrlRepository urlRepository;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L).name("jane").email("jane@example.com")
                .password("hash").role("ROLE_USER").build();
    }

    /** The service reads the last id in its constructor, so stub that first. */
    private UrlService newService(Optional<Url> lastUrl) {
        when(urlRepository.findTopByOrderByIdDesc()).thenReturn(lastUrl);
        return new UrlService(urlRepository);
    }

    @Test
    void shortenUrl_createsNewCodeStartingAt100000() {
        UrlService service = newService(Optional.empty());
        when(urlRepository.findByLongUrlAndUser("https://example.com", user))
                .thenReturn(Optional.empty());

        String code = service.shortenUrl("https://example.com", user);

        assertEquals(Base62Encoder.encode(100_000L), code);

        ArgumentCaptor<Url> saved = ArgumentCaptor.forClass(Url.class);
        verify(urlRepository).save(saved.capture());
        assertEquals(code, saved.getValue().getShortCode());
        assertEquals("https://example.com", saved.getValue().getLongUrl());
        assertSame(user, saved.getValue().getUser());
    }

    @Test
    void shortenUrl_incrementsCounterForEachNewUrl() {
        UrlService service = newService(Optional.empty());
        when(urlRepository.findByLongUrlAndUser(any(), any())).thenReturn(Optional.empty());

        String first = service.shortenUrl("https://a.com", user);
        String second = service.shortenUrl("https://b.com", user);

        assertEquals(Base62Encoder.encode(100_000L), first);
        assertEquals(Base62Encoder.encode(100_001L), second);
    }

    @Test
    void shortenUrl_continuesFromLastDatabaseId() {
        Url last = new Url();
        ReflectionTestUtils.setField(last, "id", 500L);
        UrlService service = newService(Optional.of(last));
        when(urlRepository.findByLongUrlAndUser(any(), any())).thenReturn(Optional.empty());

        assertEquals(Base62Encoder.encode(501L), service.shortenUrl("https://a.com", user));
    }

    @Test
    void shortenUrl_returnsExistingCodeForDuplicateUrl() {
        UrlService service = newService(Optional.empty());
        Url existing = new Url();
        existing.setShortCode("abc");
        existing.setLongUrl("https://example.com");
        when(urlRepository.findByLongUrlAndUser("https://example.com", user))
                .thenReturn(Optional.of(existing));

        assertEquals("abc", service.shortenUrl("https://example.com", user));
        verify(urlRepository, never()).save(any());
    }

    @Test
    void getLongUrl_returnsStoredUrl() {
        UrlService service = newService(Optional.empty());
        Url url = new Url();
        url.setShortCode("abc");
        url.setLongUrl("https://example.com");
        when(urlRepository.findByShortCode("abc")).thenReturn(Optional.of(url));

        assertEquals("https://example.com", service.getLongUrl("abc"));
    }

    @Test
    void getLongUrl_returnsNullWhenCodeUnknown() {
        UrlService service = newService(Optional.empty());
        when(urlRepository.findByShortCode("nope")).thenReturn(Optional.empty());

        assertNull(service.getLongUrl("nope"));
    }

    @Test
    void getUrlsByUser_mapsEntitiesToResponses() {
        UrlService service = newService(Optional.empty());
        Url url = new Url();
        url.setShortCode("abc");
        url.setLongUrl("https://example.com");
        when(urlRepository.findByUser(user)).thenReturn(List.of(url));

        List<UrlResponse> result = service.getUrlsByUser(user);

        assertEquals(1, result.size());
        assertEquals("abc", result.get(0).shortCode());
        assertEquals("https://example.com", result.get(0).longUrl());
    }

    @Test
    void deleteUserUrl_deletesOwnedUrl() {
        UrlService service = newService(Optional.empty());
        Url url = new Url();
        url.setShortCode("abc");
        when(urlRepository.findByShortCodeAndUser("abc", user)).thenReturn(Optional.of(url));

        service.deleteUserUrl("abc", user);

        verify(urlRepository).delete(url);
    }

    @Test
    void deleteUserUrl_throwsWhenUrlNotOwnedOrMissing() {
        UrlService service = newService(Optional.empty());
        when(urlRepository.findByShortCodeAndUser("abc", user)).thenReturn(Optional.empty());

        assertThrows(UrlNotFoundException.class, () -> service.deleteUserUrl("abc", user));
        verify(urlRepository, never()).delete(any());
    }
}
