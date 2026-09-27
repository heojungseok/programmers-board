package com.board.post;

import com.board.post.dto.PostListItemResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PostRepository extends JpaRepository<Post, Long> {

    @Query(value = """
            select new com.board.post.dto.PostListItemResponse(
                    p.id, p.title, m.nickname, count(c), p.createdAt)
            from Post p
                join p.author m
                left join Comment c on c.post = p and c.deletedAt is null
            where p.deletedAt is null
            group by p.id, p.title, m.nickname, p.createdAt
            order by p.createdAt desc
            """,
            countQuery = """
            select count(p) from Post p where p.deletedAt is null 
                    """)
    Page<PostListItemResponse> findList(Pageable pageable);
}
