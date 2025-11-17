package com.melodical.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "comments", indexes = {
    @Index(name = "idx_musical_id", columnList = "musical_id"),
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_parent_id", columnList = "parent_id"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "musical_id", nullable = false)
    private Musical musical;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    // 대댓글 관계: 부모 댓글 (null이면 최상위 댓글)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent;

    // 자식 댓글 목록 (대댓글들)
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Comment> replies = new ArrayList<>();

    // 대댓글 깊이 (0: 최상위, 1: 1단계 대댓글, 2: 2단계 대댓글 등)
    @Column(name = "depth", nullable = false)
    @Builder.Default
    private Integer depth = 0;

    // 삭제 여부 (실제 삭제 대신 소프트 삭제)
    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    // 좋아요 수 (캐시용)
    @Column(name = "like_count", nullable = false)
    @Builder.Default
    private Integer likeCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (depth == null) {
            depth = parent != null ? parent.getDepth() + 1 : 0;
        }
        if (isDeleted == null) {
            isDeleted = false;
        }
        if (likeCount == null) {
            likeCount = 0;
        }
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // 대댓글 추가 편의 메서드
    public void addReply(Comment reply) {
        replies.add(reply);
        reply.setParent(this);
        reply.setDepth(this.depth + 1);
    }

    // 삭제 처리 (소프트 삭제)
    public void softDelete() {
        this.isDeleted = true;
        this.content = "삭제된 댓글입니다.";
        this.updatedAt = LocalDateTime.now();
    }
}
