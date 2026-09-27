package com.board.global;

import com.board.support.IntegrationTest;
import com.board.support.TestFixture;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import tools.jackson.databind.JsonNode;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorResponseTest extends IntegrationTest {

    @Test
    void 깨진_JSON은_400과_공통_형식으로_답한다() {
        String token = TestFixture.signUpAndLogin(testRestTemplate, "error-json@board.com");

        ResponseEntity<JsonNode> response = testRestTemplate.exchange("/api/posts", HttpMethod.POST,
                new HttpEntity<>("{\"title\":", TestFixture.jsonHeaders(token)), JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("code").asString()).isEqualTo("INVALID_INPUT");
    }

    @Test
    void 경로_변수_타입이_틀리면_400() {
        ResponseEntity<JsonNode> response = testRestTemplate.getForEntity("/api/posts/abc", JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("code").asString()).isEqualTo("INVALID_INPUT");
        assertThat(response.getBody().get("errors").get(0).get("field").asString()).isEqualTo("postId");
    }

    @Test
    void 없는_주소는_404와_공통_형식으로_답한다() {
        String token = TestFixture.signUpAndLogin(testRestTemplate, "error-path@board.com");

        ResponseEntity<JsonNode> response = testRestTemplate.exchange("/api/nope", HttpMethod.GET,
                new HttpEntity<>(TestFixture.bearerHeaders(token)), JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().get("code").asString()).isEqualTo("RESOURCE_NOT_FOUND");
    }

    @Test
    void 지원하지_않는_요청_형식은_415() {
        String token = TestFixture.signUpAndLogin(testRestTemplate, "error-media@board.com");

        HttpHeaders headers = TestFixture.bearerHeaders(token);
        headers.setContentType(MediaType.TEXT_PLAIN);

        ResponseEntity<JsonNode> response = testRestTemplate.exchange("/api/posts", HttpMethod.POST,
                new HttpEntity<>("hello", headers), JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertThat(response.getBody().get("code").asString()).isEqualTo("UNSUPPORTED_MEDIA_TYPE");
    }

    @Test
    void 모르는_정렬_파라미터는_무시되고_500이_나지_않는다() {
        String token = TestFixture.signUpAndLogin(testRestTemplate, "error-sort@board.com");
        TestFixture.createPost(testRestTemplate, token, "정렬 파라미터 확인용 글");

        ResponseEntity<JsonNode> response =
                testRestTemplate.getForEntity("/api/posts?page=0&size=5&sort=nope", JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("data").get("content")).isNotEmpty();
    }

}
