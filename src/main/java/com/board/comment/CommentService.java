package com.board.comment;

import com.board.comment.dto.CommentCreateRequest;
import com.board.comment.dto.CommentResponse;
import com.board.comment.dto.CommentUpdateRequest;
import com.board.global.exception.BusinessException;
import com.board.global.exception.ErrorCode;
import com.board.member.Member;
import com.board.member.MemberRepository;
import com.board.post.Post;
import com.board.post.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public CommentResponse create(Long memberId, Long postId, CommentCreateRequest request) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
        Member author = memberRepository.getReferenceById(memberId);

        return CommentResponse.from(
                commentRepository.save(new Comment(request.content(), post, author))
        );
    }

    public List<CommentResponse> list(Long postId) {
        if (!postRepository.existsById(postId)) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }
        return commentRepository.findResponsesByPostId(postId);
    }

    @Transactional
    public CommentResponse update(Long memberId, Long commentId, CommentUpdateRequest request) {
        Comment comment = getOwnedComment(memberId, commentId);

        comment.update(request.content());

        return CommentResponse.from(comment);
    }

    @Transactional
    public void delete(Long memberId, Long commentId) {
        getOwnedComment(memberId, commentId).softDelete(Instant.now());
    }

    private Comment getOwnedComment(Long memberId, Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));

        if (!comment.isAuthor(memberId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        return comment;
    }
}
