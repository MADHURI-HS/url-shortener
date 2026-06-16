package com.urlshortener.repository;

import com.urlshortener.model.Url;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UrlRepository extends JpaRepository<Url, Long> {
    // Spring generates: SELECT * FROM urls WHERE short_code = ?
    Optional<Url> findByShortCode(String shortCode);

    // Spring generates: SELECT * FROM urls WHERE long_url = ?
    Optional<Url> findByLongUrl(String longUrl);

    Optional<Url> findTopByOrderByIdDesc();
}