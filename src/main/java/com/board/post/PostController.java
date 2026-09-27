package com.board.post;

import com.board.global.response.ApiResponse;
import com.board.global.response.PageResponse;
import com.board.post.dto.PostDetailResponse;
import com.board.post.dto.PostCreateRequest;
import com.board.post.dto.PostListItemResponse;
import com.board.post.dto.PostResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @GetMapping("/{postId}")
    public ApiResponse<PostDetailResponse> detail(@PathVariable Long postId) {

        return ApiResponse.success("SUCCESS", "상세 조회 완료", postService.detail(postId));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PostResponse>> create(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody PostCreateRequest request) {

        PostResponse response = postService.create(memberId, request);
        return ResponseEntity
                .created(URI.create("/api/posts/" + response.getId()))
                .body(
                        ApiResponse.success("SUCCESS", "글 생성 완료", response)
                );
    }

    @GetMapping
    public ApiResponse<PageResponse<PostListItemResponse>> list(Pageable pageable) {
        return ApiResponse.success("SUCCESS", "목록 조회", postService.list(pageable));
    }
}
