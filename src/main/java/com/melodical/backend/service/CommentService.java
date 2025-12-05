package com.melodical.backend.service;

import com.melodical.backend.dto.CommentRequestDto;
import com.melodical.backend.dto.CommentResponseDto;
import com.melodical.backend.entity.Comment;
import com.melodical.backend.entity.CommentLike;
import com.melodical.backend.entity.Musical;
import com.melodical.backend.entity.User;
import com.melodical.backend.repository.CommentRepository;
import com.melodical.backend.repository.CommentLikeRepository;
import com.melodical.backend.repository.MusicalRepository;
import com.melodical.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final MusicalRepository musicalRepository;
    private final UserRepository userRepository;
    private final CommentLikeRepository commentLikeRepository;

    @Transactional(timeout = 5) // 5초 타임아웃 설정
    public CommentResponseDto createComment(CommentRequestDto request, String email) {
        log.info("📝 Creating comment - musicalId: {}, parentId: {}, content length: {}", 
            request.getMusicalId(), request.getParentId(), request.getContent().length());
        
        try {
            // email은 JWT subject (사용자 이메일)
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email));

            Musical musical = musicalRepository.findById(request.getMusicalId())
                    .orElseThrow(() -> new IllegalArgumentException("뮤지컬을 찾을 수 없습니다"));

            Comment.CommentBuilder builder = Comment.builder()
                    .musical(musical)
                    .user(user)
                    .content(request.getContent())
                    .createdAt(LocalDateTime.now());

            // 대댓글인 경우 부모 댓글 설정
            if (request.getParentId() != null) {
                log.info("🔗 This is a reply to comment ID: {}", request.getParentId());
                Comment parent = commentRepository.findById(request.getParentId())
                        .orElseThrow(() -> new IllegalArgumentException("부모 댓글을 찾을 수 없습니다"));
                builder.parent(parent);
                builder.depth(parent.getDepth() + 1);
                log.info("✅ Parent comment found - depth will be: {}", parent.getDepth() + 1);
            } else {
                builder.depth(0);
                log.info("📌 This is a top-level comment");
            }

            Comment comment = builder.build();
            Comment savedComment = commentRepository.save(comment);
            log.info("✅ Comment saved - ID: {}, parentId: {}, depth: {}", 
                savedComment.getId(), 
                savedComment.getParent() != null ? savedComment.getParent().getId() : "null",
                savedComment.getDepth());
            
            return toResponseDto(savedComment);
            
        } catch (Exception e) {
            log.error("❌ Failed to create comment: {}", e.getMessage(), e);
            throw new RuntimeException("댓글 생성에 실패했습니다: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public List<CommentResponseDto> getCommentsByMusical(Long musicalId) {
        return commentRepository.findByMusicalId(musicalId)
                .stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CommentResponseDto> getCommentsByUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email));
        
        return commentRepository.findByUserId(user.getId())
                .stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public CommentResponseDto updateComment(Long commentId, String content, String email) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다"));
        
        // email은 JWT subject (사용자 이메일)
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email));

        if (!comment.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("권한이 없습니다");
        }

        comment.setContent(content);
        comment.setUpdatedAt(LocalDateTime.now());

        return toResponseDto(commentRepository.save(comment));
    }

    @Transactional
    public void deleteComment(Long commentId, String email) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다"));

        // email은 JWT subject (사용자 이메일)
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email));

        if (!comment.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("권한이 없습니다");
        }

        commentRepository.delete(comment);
    }

    @Transactional
    public boolean toggleLike(String email, Long commentId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email));
        
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다"));

        // 이미 좋아요한 경우 좋아요 취소
        if (commentLikeRepository.existsByUserIdAndCommentId(user.getId(), commentId)) {
            commentLikeRepository.deleteByUserIdAndCommentId(user.getId(), commentId);
            comment.setLikeCount(Math.max(0, comment.getLikeCount() - 1));
            commentRepository.save(comment);
            return false; // unliked
        } else {
            // 좋아요 추가
            CommentLike like = CommentLike.builder()
                    .user(user)
                    .comment(comment)
                    .build();
            commentLikeRepository.save(like);
            comment.setLikeCount(comment.getLikeCount() + 1);
            commentRepository.save(comment);
            return true; // liked
        }
    }

    @Transactional(readOnly = true)
    public boolean isLiked(String email, Long commentId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email));
        
        return commentLikeRepository.existsByUserIdAndCommentId(user.getId(), commentId);
    }

    @Transactional(readOnly = true)
    public long getLikeCount(Long commentId) {
        return commentLikeRepository.countByCommentId(commentId);
    }


    private CommentResponseDto toResponseDto(Comment comment) {
        return CommentResponseDto.builder()
                .id(comment.getId())
                .musicalId(comment.getMusical().getId())
                .musicalTitle(comment.getMusical().getTitle())  // 뮤지컬 제목 추가
                .userId(comment.getUser().getId())
                .username(comment.getUser().getNickname())
                .profileImageUrl(comment.getUser().getProfileImageUrl())  // 프로필 이미지 URL 추가
                .content(comment.getContent())
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .depth(comment.getDepth())
                .isDeleted(comment.getIsDeleted())
                .likeCount(comment.getLikeCount())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}
