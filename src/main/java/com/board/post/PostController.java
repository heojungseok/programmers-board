package com.board.post;

import com.board.global.response.ApiResponse;
import com.board.post.dto.PostResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @GetMapping("/{postId}")
    public ApiResponse<PostResponse> detail(@PathVariable Long postId) {

        return ApiResponse.success("SUCCESS", "상세 조회 완료", postService.detail(postId));
    }
}
