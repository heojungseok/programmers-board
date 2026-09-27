package com.board.comment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
                update Comment c 
                set c.deletedAt = :now 
                where c.post.id = :postId 
                and c.deletedAt is null
            """)
    int softDeleteByPostId(Long postId, Instant now);
}
