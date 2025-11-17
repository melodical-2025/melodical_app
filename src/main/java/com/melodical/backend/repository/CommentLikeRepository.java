package com.melodical.backend.repository;

import com.melodical.backend.entity.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {
    
    // 특정 사용자가 특정 댓글에 좋아요를 눌렀는지 확인
    boolean existsByUserIdAndCommentId(Long userId, Long commentId);
    
    // 특정 사용자의 특정 댓글 좋아요 조회
    Optional<CommentLike> findByUserIdAndCommentId(Long userId, Long commentId);
    
    // 댓글의 좋아요 개수
    long countByCommentId(Long commentId);
    
    // 사용자가 좋아요한 댓글 ID 목록
    List<Long> findCommentIdsByUserId(Long userId);
    
    // 사용자와 댓글로 삭제
    void deleteByUserIdAndCommentId(Long userId, Long commentId);
}
