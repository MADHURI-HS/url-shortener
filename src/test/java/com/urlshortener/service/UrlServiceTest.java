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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UrlServiceTest {

    @Mock
    private UrlRepository urlRepository;

    @InjectMocks
    private UrlService service;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L).name("jane").email("jane@example.com")
                .password("hash").role("ROLE_USER").build();
    }

    /** Simulates the database: saveAndFlush assigns an auto-increment id (starting at firstId). */
    private void databaseAssignsIdsFrom(long firstId) {
        AtomicLong nextId = new AtomicLong(firstId);
        when(urlRepository.saveAndFlush(any(Url.class))).thenAnswer(invocation -> {
            Url url = invocation.getArgument(0);
            ReflectionTestUtils.setField(url, "id", nextId.getAndIncrement());
            return url;
        });
    }

    @Test
    void shortenUrl_buildsCodeFromDatabaseIdPlusOffset() {
        databaseAssignsIdsFrom(7L);
        when(urlRepository.findByLongUrlAndUser("https://example.com", user))
                .thenReturn(Optional.empty());

        String code = service.shortenUrl("https://example.com", user);

        assertEquals(Base62Encoder.encode(UrlService.CODE_OFFSET + 7L), code);
    }

    @Test
    void shortenUrl_savesLongUrlAndOwnerAndFinalCode() {
        databaseAssignsIdsFrom(1L);
        when(urlRepository.findByLongUrlAndUser(any(), any())).thenReturn(Optional.empty());

        String code = service.shortenUrl("https://example.com", user);

        ArgumentCaptor<Url> saved = ArgumentCaptor.forClass(Url.class);
        verify(urlRepository).save(saved.capture());
        assertEquals(code, saved.getValue().getShortCode());
        assertEquals("https://example.com", saved.getValue().getLongUrl());
        assertSame(user, saved.getValue().getUser());
    }

    @Test
    void shortenUrl_differentUrlsGetDifferentCodes() {
        databaseAssignsIdsFrom(1L);
        when(urlRepository.findByLongUrlAndUser(any(), any())).thenReturn(Optional.empty());

        String first = service.shortenUrl("https://a.com", user);
        String second = service.shortenUrl("https://b.com", user);

        assertEquals(Base62Encoder.encode(UrlService.CODE_OFFSET + 1L), first);
        assertEquals(Base62Encoder.encode(UrlService.CODE_OFFSET + 2L), second);
        assertNotEquals(first, second);
    }

    @Test
    void shortenUrl_returnsExistingCodeForDuplicateUrl() {
        Url existing = new Url();
        existing.setShortCode("abc");
        existing.setLongUrl("https://example.com");
        when(urlRepository.findByLongUrlAndUser("https://example.com", user))
                .thenReturn(Optional.of(existing));

        assertEquals("abc", service.shortenUrl("https://example.com", user));
        verify(urlRepository, never()).saveAndFlush(any());
        verify(urlRepository, never()).save(any());
    }

    @Test
    void getLongUrl_returnsStoredUrl() {
        Url url = new Url();
        url.setShortCode("abc");
        url.setLongUrl("https://example.com");
        when(urlRepository.findByShortCode("abc")).thenReturn(Optional.of(url));

        assertEquals("https://example.com", service.getLongUrl("abc"));
    }

    @Test
    void getLongUrl_returnsNullWhenCodeUnknown() {
        when(urlRepository.findByShortCode("nope")).thenReturn(Optional.empty());

        assertNull(service.getLongUrl("nope"));
    }

    @Test
    void getUrlsByUser_mapsEntitiesToResponses() {
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
        Url url = new Url();
        url.setShortCode("abc");
        when(urlRepository.findByShortCodeAndUser("abc", user)).thenReturn(Optional.of(url));

        service.deleteUserUrl("abc", user);

        verify(urlRepository).delete(url);
    }

    @Test
    void deleteUserUrl_throwsWhenUrlNotOwnedOrMissing() {
        when(urlRepository.findByShortCodeAndUser("abc", user)).thenReturn(Optional.empty());

        assertThrows(UrlNotFoundException.class, () -> service.deleteUserUrl("abc", user));
        verify(urlRepository, never()).delete(any());
    }
}
