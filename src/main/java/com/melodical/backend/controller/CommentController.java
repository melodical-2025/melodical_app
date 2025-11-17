package com.melodical.backend.controller;

import com.melodical.backend.dto.CommentRequestDto;
import com.melodical.backend.dto.CommentResponseDto;
import com.melodical.backend.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;

    /**
     * 뮤지컬별 댓글 목록 조회 (인증 불필요)
     */
    @GetMapping("/musical/{musicalId}")
    public ResponseEntity<List<CommentResponseDto>> getCommentsByMusical(
            @PathVariable Long musicalId) {
        List<CommentResponseDto> comments = commentService.getCommentsByMusical(musicalId);
        return ResponseEntity.ok(comments);
    }

    /**
     * 내가 작성한 댓글 목록 조회 (인증 필요)
     */
    @GetMapping("/my")
    public ResponseEntity<List<CommentResponseDto>> getMyComments(
            @AuthenticationPrincipal UserDetails userDetails) {
        List<CommentResponseDto> comments = commentService.getCommentsByUser(userDetails.getUsername());
        return ResponseEntity.ok(comments);
    }

    /**
     * 댓글 작성 (인증 필요)
     */
    @PostMapping
    public ResponseEntity<CommentResponseDto> createComment(
            @RequestBody CommentRequestDto request,
            @AuthenticationPrincipal UserDetails userDetails) {
        CommentResponseDto comment = commentService.createComment(request, userDetails.getUsername());
        return ResponseEntity.ok(comment);
    }

    /**
     * 댓글 수정 (인증 필요)
     */
    @PutMapping("/{commentId}")
    public ResponseEntity<CommentResponseDto> updateComment(
            @PathVariable Long commentId,
            @RequestBody String content,
            @AuthenticationPrincipal UserDetails userDetails) {
        CommentResponseDto comment = commentService.updateComment(commentId, content, userDetails.getUsername());
        return ResponseEntity.ok(comment);
    }

    /**
     * 댓글 삭제 (인증 필요)
     */
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal UserDetails userDetails) {
        commentService.deleteComment(commentId, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    /**
     * 댓글 좋아요 토글 (인증 필요)
     * @return true = 좋아요 추가, false = 좋아요 취소
     */
    @PostMapping("/{commentId}/like")
    public ResponseEntity<Boolean> toggleLike(
            @PathVariable Long commentId,
            @AuthenticationPrincipal UserDetails userDetails) {
        boolean liked = commentService.toggleLike(userDetails.getUsername(), commentId);
        return ResponseEntity.ok(liked);
    }

    /**
     * 댓글 좋아요 여부 확인 (인증 필요)
     */
    @GetMapping("/{commentId}/liked")
    public ResponseEntity<Boolean> isLiked(
            @PathVariable Long commentId,
            @AuthenticationPrincipal UserDetails userDetails) {
        boolean liked = commentService.isLiked(userDetails.getUsername(), commentId);
        return ResponseEntity.ok(liked);
    }

    /**
     * 댓글 좋아요 개수 조회 (인증 불필요)
     */
    @GetMapping("/{commentId}/like-count")
    public ResponseEntity<Long> getLikeCount(@PathVariable Long commentId) {
        long count = commentService.getLikeCount(commentId);
        return ResponseEntity.ok(count);
    }
}
