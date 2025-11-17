package com.melodical.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentRequestDto {
    private Long musicalId;
    private String content;
    private Long parentId;  // 대댓글인 경우 부모 댓글 ID (null이면 최상위 댓글)
}
