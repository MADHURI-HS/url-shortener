-- MySQL's default collation (utf8mb4_0900_ai_ci) is CASE-INSENSITIVE.
-- Base62 short codes and URL paths are CASE-SENSITIVE, so these two columns
-- must use a binary collation. Without this, codes such as "q1a" and "q1A"
-- collide on the unique index, and "https://x.com/Doc" equals "https://x.com/doc".
-- Runs after Hibernate has created the tables (see spring.jpa.defer-datasource-initialization)
-- and is safe to run on every start.
ALTER TABLE urls MODIFY short_code VARCHAR(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL;
ALTER TABLE urls MODIFY long_url VARCHAR(2048) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL;
