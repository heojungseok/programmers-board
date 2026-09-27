package com.board.comment;

import com.board.comment.dto.CommentCreateRequest;
import com.board.comment.dto.CommentResponse;
import com.board.comment.dto.CommentUpdateRequest;
import com.board.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping("/api/posts/{postId}/comments")
    public ResponseEntity<ApiResponse<CommentResponse>> create(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long postId,
            @Valid @RequestBody CommentCreateRequest request) {

        CommentResponse response = commentService.create(memberId, postId, request);

        return ResponseEntity
                .created(URI.create("/api/comments/" + response.id()))
                .body(ApiResponse.success("SUCCESS", "댓글 작성 완료", response));
    }

    @GetMapping("/api/posts/{postId}/comments")
    public ApiResponse<List<CommentResponse>> list(@PathVariable Long postId) {

        return ApiResponse.success("SUCCESS", "댓글 목록 조회 완료", commentService.list(postId));
    }

    @PutMapping("/api/comments/{commentId}")
    public ApiResponse<CommentResponse> update(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long commentId,
            @Valid @RequestBody CommentUpdateRequest request) {

        return ApiResponse.success("SUCCESS", "댓글 수정 완료",
                commentService.update(memberId, commentId, request));
    }

    @DeleteMapping("/api/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Long memberId,
                       @PathVariable Long commentId) {

        commentService.delete(memberId, commentId);
    }
}
