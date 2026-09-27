package com.board.post.dto;

import java.time.Instant;

public record PostListItemResponse(Long id,
                                   String title,
                                   String nickname,
                                   Long commentCount,
                                   Instant createdAt) {

}
