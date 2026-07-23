package com.urlshortener.repository;

import com.urlshortener.model.Url;
import com.urlshortener.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UrlRepository extends JpaRepository<Url, Long> {

    Optional<Url> findByShortCode(String shortCode);

    Optional<Url> findTopByOrderByIdDesc();

    Optional<Url> findByLongUrlAndUser(String longUrl, User user);

    Optional<Url> findByShortCodeAndUser(String shortCode, User user);

    List<Url> findByUser(User user);

}