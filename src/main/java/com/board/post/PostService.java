package com.board.post;

import com.board.global.exception.BusinessException;
import com.board.global.exception.ErrorCode;
import com.board.member.Member;
import com.board.member.MemberRepository;
import com.board.post.dto.PostDetailResponse;
import com.board.post.dto.PostCreateRequest;
import com.board.post.dto.PostResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final MemberRepository memberRepository;

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
}
