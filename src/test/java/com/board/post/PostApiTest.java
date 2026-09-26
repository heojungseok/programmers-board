package com.board.post;

import com.board.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class PostApiTest extends IntegrationTest {

    @Test
    void 없는_글을_조회하면_404와_오류_봉투() {
        ResponseEntity<String> response = testRestTemplate.getForEntity("/api/posts/999999", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).contains("POST_NOT_FOUND");
    }
}
