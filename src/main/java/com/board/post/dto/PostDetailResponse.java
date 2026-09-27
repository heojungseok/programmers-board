package com.board.post.dto;

import com.board.post.Post;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class PostDetailResponse {

    private Long id;
    private String title;
    private String content;
    private String nickname;

    public static PostDetailResponse from(Post post) {
        return new PostDetailResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getAuthor().getNickname()
        );
    }

}
