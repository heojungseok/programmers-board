package com.board.comment;

import com.board.comment.dto.CommentResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Comment c
            set c.deletedAt = :now
            where c.post.id = :postId
            and c.deletedAt is null
            """)
    int softDeleteByPostId(Long postId, Instant now);

    @Query("""
            select new com.board.comment.dto.CommentResponse(
                    c.id, c.content, m.nickname, c.createdAt)
            from Comment c
                join c.author m
            where c.post.id = :postId
            and c.deletedAt is null
            order by c.id asc
            """)
    List<CommentResponse> findResponsesByPostId(Long postId);
}
