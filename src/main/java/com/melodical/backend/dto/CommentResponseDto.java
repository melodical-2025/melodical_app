package com.melodical.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentResponseDto {
    private Long id;
    private Long musicalId;
    private String musicalTitle;  // 뮤지컬 제목 추가
    private Long userId;
    private String username;
    private String content;
    
    // 대댓글 관련 필드
    private Long parentId;  // 부모 댓글 ID (null이면 최상위 댓글)
    private Integer depth;  // 대댓글 깊이 (0: 최상위)
    private Boolean isDeleted;  // 삭제 여부
    private Integer likeCount;  // 좋아요 수
    
    @Builder.Default
    private List<CommentResponseDto> replies = new ArrayList<>();  // 자식 댓글 목록
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
