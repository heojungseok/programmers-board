package com.board.support;

import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import tools.jackson.databind.JsonNode;

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
}
