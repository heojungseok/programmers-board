package com.board.member;

import com.board.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import tools.jackson.databind.JsonNode;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

;

class AuthApiTest extends IntegrationTest {

    @Test
    void 가입하면_201과_비밀번호_없는_응답() {
        ResponseEntity<JsonNode> response = signUp("new@board.com", "password123");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        JsonNode data = response.getBody().get("data");
        assertThat(data.get("email").asString()).isEqualTo("new@board.com");
        assertThat(data.get("nickname").asString()).isEqualTo("정석");
        assertThat(data.has("password")).isFalse();
    }

    @Test
    void 이메일_형식이_아니면_400() {
        ResponseEntity<JsonNode> response = signUp("not-an-email", "password123");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("code").asString()).isEqualTo("INVALID_INPUT");
        assertThat(response.getBody().get("errors").get(0).get("field").asString()).isEqualTo("email");
    }

    @Test
    void 비밀번호가_짧으면_400() {
        ResponseEntity<JsonNode> response = signUp("short@board.com", "1234567");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("errors").get(0).get("field").asString()).isEqualTo("password");
    }

    @Test
    void 이메일이_중복이면_409() {
        signUp("dup@board.com", "password123");

        ResponseEntity<JsonNode> response = signUp("dup@board.com", "password123");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().get("code").asString()).isEqualTo("EMAIL_DUPLICATED");
    }

    private ResponseEntity<JsonNode> signUp(String email, String password) {
        return testRestTemplate.postForEntity("/api/members",
                Map.of("email", email, "password", password, "nickname", "정석"),
                JsonNode.class);
    }
}
