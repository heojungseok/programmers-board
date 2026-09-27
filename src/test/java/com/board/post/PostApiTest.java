package com.board.post;

import com.board.support.IntegrationTest;
import com.board.support.TestFixture;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PostApiTest extends IntegrationTest {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

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

    @Test
    void 글을_작성하면_201과_Location() {
        String token = TestFixture.signUpAndLogin(testRestTemplate, "writer@board.com");

        ResponseEntity<JsonNode> response = createPost(token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        long postId = response.getBody().get("data").get("id").asLong();
        assertThat(response.getHeaders().getLocation()).isNotNull();
        assertThat(response.getHeaders().getLocation().toString()).endsWith("/api/posts/" + postId);
    }

    @Test
    void 글_상세는_로그인_없이_조회된다() {
        String token = TestFixture.signUpAndLogin(testRestTemplate, "reader@board.com");
        long postId = createPost(token).getBody().get("data").get("id").asLong();

        ResponseEntity<JsonNode> response = testRestTemplate.getForEntity("/api/posts/" + postId, JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode data = response.getBody().get("data");
        assertThat(data.get("title").asString()).isEqualTo("제목");
        assertThat(data.get("authorNickname").asString()).isEqualTo(TestFixture.NICKNAME);
    }

    @Test
    void 목록은_최신순_페이지로_나온다() {
        String token = TestFixture.signUpAndLogin(testRestTemplate, "list@board.com");
        createPost(token, "먼저 쓴 글");
        createPost(token, "나중에 쓴 글");

        ResponseEntity<JsonNode> response = testRestTemplate.getForEntity("/api/posts?page=0&size=50", JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode data = response.getBody().get("data");
        assertThat(data.get("totalElements").asLong()).isGreaterThanOrEqualTo(2);

        List<String> titles = titlesOf(data.get("content"));
        assertThat(titles.indexOf("나중에 쓴 글")).isLessThan(titles.indexOf("먼저 쓴 글"));
    }

    @Test
    void 목록에는_작성자_닉네임과_댓글_수가_담긴다() {
        String token = TestFixture.signUpAndLogin(testRestTemplate, "listitem@board.com");
        createPost(token, "항목 확인용 글");

        ResponseEntity<JsonNode> response = testRestTemplate.getForEntity("/api/posts?page=0&size=50", JsonNode.class);

        JsonNode item = itemByTitle(response.getBody().get("data").get("content"), "항목 확인용 글");
        assertThat(item.get("nickname").asString()).isEqualTo(TestFixture.NICKNAME);
        assertThat(item.get("commentCount").asLong()).isZero();
    }

    @Test
    void 목록_조회_쿼리_수가_글_개수와_무관하다() {
        String token = TestFixture.signUpAndLogin(testRestTemplate, "querycount@board.com");
        for (int i = 0; i < 5; i++) {
            createPost(token, "쿼리 수 확인 " + i);
        }

        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        long before = statistics.getPrepareStatementCount();

        ResponseEntity<JsonNode> response = testRestTemplate.getForEntity("/api/posts?page=0&size=50", JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(statistics.getPrepareStatementCount() - before).isLessThanOrEqualTo(2);
    }

    private List<String> titlesOf(JsonNode content) {
        List<String> titles = new ArrayList<>();
        for (int i = 0; i < content.size(); i++) {
            titles.add(content.get(i).get("title").asString());
        }
        return titles;
    }

    private JsonNode itemByTitle(JsonNode content, String title) {
        for (int i = 0; i < content.size(); i++) {
            if (title.equals(content.get(i).get("title").asString())) {
                return content.get(i);
            }
        }
        throw new AssertionError("목록에 없는 제목: " + title);
    }

    private ResponseEntity<JsonNode> createPost(String token, String title) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        return testRestTemplate.exchange("/api/posts", HttpMethod.POST,
                new HttpEntity<>(Map.of("title", title, "content", "본문"), headers),
                JsonNode.class);
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
