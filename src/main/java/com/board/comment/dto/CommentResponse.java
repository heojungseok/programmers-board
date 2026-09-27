package com.board.comment.dto;

import com.board.comment.Comment;

import java.time.Instant;

public record CommentResponse(Long id,
                              String content,
                              String nickname,
                              Instant createdAt) {

    public static CommentResponse from(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getContent(),
                comment.getAuthor().getNickname(),
                comment.getCreatedAt()
        );
    }
}
