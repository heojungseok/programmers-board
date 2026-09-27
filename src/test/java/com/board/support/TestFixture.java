package com.board.support;

import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class TestFixture {

    public static final String PASSWORD = "password123";
    public static final String NICKNAME = "정석";

    private TestFixture() {
    }

    public static String signUpAndLogin(TestRestTemplate testRestTemplate, String email) {
        testRestTemplate.postForEntity("/api/members",
                Map.of("email", email, "password", PASSWORD, "nickname", NICKNAME),
                String.class);

        ResponseEntity<JsonNode> login = testRestTemplate.postForEntity("/api/auth/login",
                Map.of("email", email, "password", PASSWORD),
                JsonNode.class);

        return login.getBody().get("data").get("accessToken").asString();
    }

    public static long createPost(TestRestTemplate testRestTemplate, String token, String title) {
        ResponseEntity<JsonNode> response = testRestTemplate.exchange("/api/posts", HttpMethod.POST,
                new HttpEntity<>(Map.of("title", title, "content", "본문"), jsonHeaders(token)),
                JsonNode.class);

        return response.getBody().get("data").get("id").asLong();
    }

    public static List<String> valuesOf(JsonNode array, String field) {
        List<String> values = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            values.add(array.get(i).get(field).asString());
        }
        return values;
    }

    public static JsonNode itemBy(JsonNode array, String field, String value) {
        for (int i = 0; i < array.size(); i++) {
            if (value.equals(array.get(i).get(field).asString())) {
                return array.get(i);
            }
        }
        throw new AssertionError(field + "=" + value + " 항목이 목록에 없습니다");
    }

    public static HttpHeaders jsonHeaders(String token) {
        HttpHeaders headers = bearerHeaders(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    public static HttpHeaders bearerHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }
}
