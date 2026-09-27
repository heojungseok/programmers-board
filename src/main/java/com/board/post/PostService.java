package com.board.post;

import com.board.comment.CommentRepository;
import com.board.global.exception.BusinessException;
import com.board.global.exception.ErrorCode;
import com.board.global.response.PageResponse;
import com.board.member.Member;
import com.board.member.MemberRepository;
import com.board.post.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final MemberRepository memberRepository;
    private final CommentRepository commentRepository;

    public PostDetailResponse detail(Long postId) {

        return PostDetailResponse.from(getPost(postId));
    }

    private Post getPost(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
    }

    @Transactional
    public PostResponse create(Long memberId, PostCreateRequest request) {
        Member author = memberRepository.getReferenceById(memberId);

        return PostResponse.from(
                postRepository.save(new Post(request.title(), request.content(), author))
        );
    }

    public PageResponse<PostListItemResponse> list(Pageable pageable) {
        return PageResponse.from(postRepository.findList(pageable));
    }

    @Transactional
    public PostResponse update(Long memberId, Long postId, PostUpdateRequest request) {
        Post post = getOwnedPost(memberId, postId);

        post.update(request.title(), request.content());

        return PostResponse.from(post);
    }

    @Transactional
    public void delete(Long memberId, Long postId) {
        Post post = getOwnedPost(memberId, postId);
        Instant now = Instant.now();
        post.softDelete(now);
        commentRepository.softDeleteByPostId(postId, now);
    }

    private Post getOwnedPost(Long memberId, Long postId) {
        Post post = getPost(postId);

        if (!post.isAuthor(memberId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        return post;
    }
}
