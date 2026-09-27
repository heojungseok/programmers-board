package com.board.post;

import com.board.support.IntegrationTest;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PostApiTest extends IntegrationTest {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Test
    void 없는_글을_조회하면_404와_오류_응답() {
        ResponseEntity<String> response = testRestTemplate.getForEntity("/api/posts/999999", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).contains("POST_NOT_FOUND");
    }

    @Test
    void 로그인_없이_글을_쓰면_401() {
        ResponseEntity<JsonNode> response = createPost(null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().get("code").asString()).isEqualTo("AUTH_REQUIRED");
    }

    @Test
    void 잘못된_토큰이면_401() {
        ResponseEntity<JsonNode> response = createPost("not-a-real-token");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void 만료된_토큰이면_TOKEN_EXPIRED() {
        ResponseEntity<JsonNode> response = createPost(expiredToken());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().get("code").asString()).isEqualTo("TOKEN_EXPIRED");
    }

    private ResponseEntity<JsonNode> createPost(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.setBearerAuth(token);
        }

        return testRestTemplate.exchange("/api/posts", HttpMethod.POST,
                new HttpEntity<>(Map.of("title", "제목", "content", "본문"), headers),
                JsonNode.class);
    }

    private String expiredToken() {
        Instant expiredAt = Instant.now().minusSeconds(60);

        return Jwts.builder()
                .subject("1")
                .issuedAt(Date.from(expiredAt.minusSeconds(60)))
                .expiration(Date.from(expiredAt))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret)))
                .compact();
    }
}
