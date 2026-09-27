package com.board.comment;

import com.board.support.IntegrationTest;
import com.board.support.TestFixture;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import tools.jackson.databind.JsonNode;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CommentApiTest extends IntegrationTest {

    @Test
    void 댓글을_작성하면_201과_Location() {
        String token = TestFixture.signUpAndLogin(testRestTemplate, "comment-create@board.com");
        long postId = TestFixture.createPost(testRestTemplate, token, "댓글 작성용 글");

        ResponseEntity<JsonNode> response = createComment(token, postId, "첫 댓글");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        JsonNode data = response.getBody().get("data");
        assertThat(data.get("nickname").asString()).isEqualTo(TestFixture.NICKNAME);
        assertThat(response.getHeaders().getLocation().toString())
                .endsWith("/api/comments/" + data.get("id").asLong());
    }

    @Test
    void 없는_글에_댓글을_쓰면_404() {
        String token = TestFixture.signUpAndLogin(testRestTemplate, "comment-nopost@board.com");

        ResponseEntity<JsonNode> response = createComment(token, 999999L, "없는 글의 댓글");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().get("code").asString()).isEqualTo("POST_NOT_FOUND");
    }

    @Test
    void 댓글_목록은_로그인_없이_오래된_순으로_전체_반환된다() {
        String token = TestFixture.signUpAndLogin(testRestTemplate, "comment-list@board.com");
        long postId = TestFixture.createPost(testRestTemplate, token, "댓글 목록용 글");
        createComment(token, postId, "먼저 쓴 댓글");
        createComment(token, postId, "나중에 쓴 댓글");

        ResponseEntity<JsonNode> response = comments(postId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(TestFixture.valuesOf(response.getBody().get("data"), "content"))
                .containsExactly("먼저 쓴 댓글", "나중에 쓴 댓글");
    }

    @Test
    void 남의_댓글을_수정하면_403() {
        String ownerToken = TestFixture.signUpAndLogin(testRestTemplate, "comment-owner@board.com");
        String otherToken = TestFixture.signUpAndLogin(testRestTemplate, "comment-other@board.com");
        long postId = TestFixture.createPost(testRestTemplate, ownerToken, "댓글 권한용 글");
        long commentId = createComment(ownerToken, postId, "주인 있는 댓글")
                .getBody().get("data").get("id").asLong();

        ResponseEntity<JsonNode> response = updateComment(otherToken, commentId, "몰래 수정");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().get("code").asString()).isEqualTo("ACCESS_DENIED");
    }

    @Test
    void 작성자는_자기_댓글을_수정한다() {
        String token = TestFixture.signUpAndLogin(testRestTemplate, "comment-edit@board.com");
        long postId = TestFixture.createPost(testRestTemplate, token, "댓글 수정용 글");
        long commentId = createComment(token, postId, "수정 전 댓글")
                .getBody().get("data").get("id").asLong();

        ResponseEntity<JsonNode> response = updateComment(token, commentId, "수정 후 댓글");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(TestFixture.valuesOf(comments(postId).getBody().get("data"), "content"))
                .containsExactly("수정 후 댓글");
    }

    @Test
    void 댓글을_삭제하면_목록에서_사라진다() {
        String token = TestFixture.signUpAndLogin(testRestTemplate, "comment-delete@board.com");
        long postId = TestFixture.createPost(testRestTemplate, token, "댓글 삭제용 글");
        createComment(token, postId, "남을 댓글");
        long removedId = createComment(token, postId, "지울 댓글").getBody().get("data").get("id").asLong();

        assertThat(deleteComment(token, removedId).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        assertThat(TestFixture.valuesOf(comments(postId).getBody().get("data"), "content")).containsExactly("남을 댓글");
        assertThat(deleteComment(token, removedId).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void 글을_삭제하면_댓글도_함께_삭제_표시된다() {
        String token = TestFixture.signUpAndLogin(testRestTemplate, "comment-cascade@board.com");
        long postId = TestFixture.createPost(testRestTemplate, token, "함께 지워질 글");
        long commentId = createComment(token, postId, "함께 지워질 댓글 1")
                .getBody().get("data").get("id").asLong();
        createComment(token, postId, "함께 지워질 댓글 2");

        ResponseEntity<JsonNode> deleted = testRestTemplate.exchange("/api/posts/" + postId,
                HttpMethod.DELETE, new HttpEntity<>(TestFixture.bearerHeaders(token)), JsonNode.class);
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<JsonNode> list = comments(postId);
        assertThat(list.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(list.getBody().get("code").asString()).isEqualTo("POST_NOT_FOUND");

        ResponseEntity<JsonNode> comment = updateComment(token, commentId, "지워진 댓글 수정");
        assertThat(comment.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(comment.getBody().get("code").asString()).isEqualTo("COMMENT_NOT_FOUND");
    }

    @Test
    void 없는_글의_댓글_목록은_404() {
        ResponseEntity<JsonNode> response = comments(999999L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().get("code").asString()).isEqualTo("POST_NOT_FOUND");
    }

    @Test
    void 댓글을_달면_글_목록의_댓글_수가_올라간다() {
        String token = TestFixture.signUpAndLogin(testRestTemplate, "comment-count@board.com");
        long postId = TestFixture.createPost(testRestTemplate, token, "댓글 수 확인용 글");
        createComment(token, postId, "댓글 하나");
        createComment(token, postId, "댓글 둘");

        ResponseEntity<JsonNode> response =
                testRestTemplate.getForEntity("/api/posts?page=0&size=50", JsonNode.class);

        JsonNode item = TestFixture.itemBy(response.getBody().get("data").get("content"), "title", "댓글 수 확인용 글");
        assertThat(item.get("commentCount").asLong()).isEqualTo(2);
    }

    private ResponseEntity<JsonNode> createComment(String token, long postId, String content) {
        return testRestTemplate.exchange("/api/posts/" + postId + "/comments", HttpMethod.POST,
                new HttpEntity<>(Map.of("content", content), TestFixture.jsonHeaders(token)),
                JsonNode.class);
    }

    private ResponseEntity<JsonNode> updateComment(String token, long commentId, String content) {
        return testRestTemplate.exchange("/api/comments/" + commentId, HttpMethod.PUT,
                new HttpEntity<>(Map.of("content", content), TestFixture.jsonHeaders(token)),
                JsonNode.class);
    }

    private ResponseEntity<JsonNode> deleteComment(String token, long commentId) {
        return testRestTemplate.exchange("/api/comments/" + commentId, HttpMethod.DELETE,
                new HttpEntity<>(TestFixture.bearerHeaders(token)), JsonNode.class);
    }

    private ResponseEntity<JsonNode> comments(long postId) {
        return testRestTemplate.getForEntity("/api/posts/" + postId + "/comments", JsonNode.class);
    }

}
