package com.urlshortener.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShortenRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private boolean isValid(String url) {
        ShortenRequest request = new ShortenRequest();
        request.setLongUrl(url);
        return validator.validate(request).isEmpty();
    }

    @Test
    void acceptsHttpAndHttpsUrls() {
        assertTrue(isValid("https://example.com/some/path?x=1"));
        assertTrue(isValid("http://example.com"));
        assertTrue(isValid("HTTPS://EXAMPLE.COM"));
    }

    @Test
    void rejectsBlankUrl() {
        assertFalse(isValid(""));
        assertFalse(isValid("   "));
        assertFalse(isValid(null));
    }

    @Test
    void rejectsDangerousOrNonHttpSchemes() {
        assertFalse(isValid("javascript:alert(1)"));
        assertFalse(isValid("ftp://example.com"));
        assertFalse(isValid("data:text/html;base64,AAAA"));
        assertFalse(isValid("example.com"));
    }

    @Test
    void rejectsUrlsWithWhitespace() {
        assertFalse(isValid("https://exa mple.com"));
    }

    @Test
    void rejectsUrlsLongerThan2048Characters() {
        assertFalse(isValid("https://example.com/" + "a".repeat(2100)));
    }
}
