package com.melodical.backend.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 데이터베이스 스키마 수정
 * - rated_musical 테이블의 외래키 제약조건 제거
 * - musical_id를 VARCHAR로 변경하여 Interpark ID 저장
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseSchemaFix {
    
    private final JdbcTemplate jdbcTemplate;
    
    @PostConstruct
    public void fixRatedMusicalSchema() {
        try {
            log.info("🔧 Checking rated_musical table schema...");
            
            // 1. 외래키 제약조건 제거
            try {
                jdbcTemplate.execute(
                    "ALTER TABLE rated_musical DROP FOREIGN KEY fk_rated_musical_musical"
                );
                log.info("✅ Removed foreign key constraint: fk_rated_musical_musical");
            } catch (Exception e) {
                if (e.getMessage().contains("check that column/key exists")) {
                    log.info("ℹ️ Foreign key constraint already removed or doesn't exist");
                } else {
                    log.warn("⚠️ Could not remove foreign key: {}", e.getMessage());
                }
            }
            
            // 2. musical_id 컬럼 타입 확인 및 변경
            try {
                // 컬럼 타입 확인
                String columnType = jdbcTemplate.queryForObject(
                    "SELECT DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS " +
                    "WHERE TABLE_SCHEMA = 'melodical_db' " +
                    "AND TABLE_NAME = 'rated_musical' " +
                    "AND COLUMN_NAME = 'musical_id'",
                    String.class
                );
                
                if ("bigint".equalsIgnoreCase(columnType)) {
                    log.info("🔄 Converting musical_id from BIGINT to VARCHAR(50)...");
                    
                    // 기존 데이터 삭제 (타입 변환 시 필요)
                    jdbcTemplate.execute("DELETE FROM rated_musical");
                    log.info("⚠️ Cleared rated_musical table for schema migration");
                    
                    // 컬럼 타입 변경
                    jdbcTemplate.execute(
                        "ALTER TABLE rated_musical MODIFY COLUMN musical_id VARCHAR(50) NOT NULL"
                    );
                    log.info("✅ Successfully converted musical_id to VARCHAR(50)");
                } else {
                    log.info("✅ musical_id is already VARCHAR type");
                }
            } catch (Exception e) {
                log.error("❌ Error modifying musical_id column: {}", e.getMessage(), e);
            }
            
            log.info("✅ rated_musical schema fix completed");
            
        } catch (Exception e) {
            log.error("❌ Error fixing rated_musical schema: {}", e.getMessage(), e);
        }
        
        // CommentLike 테이블 생성
        createCommentLikeTable();
        
        // Comment 테이블 parent_id, depth 등 컬럼 확인 및 추가
        updateCommentTable();
    }
    
    private void updateCommentTable() {
        try {
            log.info("🔧 Checking comments table schema...");
            
            // parent_id 컬럼 존재 여부 확인
            Integer parentIdExists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_SCHEMA = 'melodical_db' " +
                "AND TABLE_NAME = 'comments' " +
                "AND COLUMN_NAME = 'parent_id'",
                Integer.class
            );
            
            if (parentIdExists == null || parentIdExists == 0) {
                log.info("🔄 Adding parent_id column to comments table...");
                jdbcTemplate.execute(
                    "ALTER TABLE comments ADD COLUMN parent_id BIGINT NULL"
                );
                jdbcTemplate.execute(
                    "ALTER TABLE comments ADD CONSTRAINT fk_comment_parent " +
                    "FOREIGN KEY (parent_id) REFERENCES comments(id) ON DELETE CASCADE"
                );
                jdbcTemplate.execute(
                    "CREATE INDEX idx_parent_id ON comments(parent_id)"
                );
                log.info("✅ parent_id column added successfully");
            } else {
                log.info("✅ parent_id column already exists");
            }
            
            // depth 컬럼 확인
            Integer depthExists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_SCHEMA = 'melodical_db' " +
                "AND TABLE_NAME = 'comments' " +
                "AND COLUMN_NAME = 'depth'",
                Integer.class
            );
            
            if (depthExists == null || depthExists == 0) {
                log.info("🔄 Adding depth column to comments table...");
                jdbcTemplate.execute(
                    "ALTER TABLE comments ADD COLUMN depth INT NOT NULL DEFAULT 0"
                );
                log.info("✅ depth column added successfully");
            } else {
                log.info("✅ depth column already exists");
            }
            
            // is_deleted 컬럼 확인
            Integer isDeletedExists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_SCHEMA = 'melodical_db' " +
                "AND TABLE_NAME = 'comments' " +
                "AND COLUMN_NAME = 'is_deleted'",
                Integer.class
            );
            
            if (isDeletedExists == null || isDeletedExists == 0) {
                log.info("🔄 Adding is_deleted column to comments table...");
                jdbcTemplate.execute(
                    "ALTER TABLE comments ADD COLUMN is_deleted BOOLEAN NOT NULL DEFAULT FALSE"
                );
                log.info("✅ is_deleted column added successfully");
            } else {
                log.info("✅ is_deleted column already exists");
            }
            
            // like_count 컬럼 확인
            Integer likeCountExists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_SCHEMA = 'melodical_db' " +
                "AND TABLE_NAME = 'comments' " +
                "AND COLUMN_NAME = 'like_count'",
                Integer.class
            );
            
            if (likeCountExists == null || likeCountExists == 0) {
                log.info("🔄 Adding like_count column to comments table...");
                jdbcTemplate.execute(
                    "ALTER TABLE comments ADD COLUMN like_count INT NOT NULL DEFAULT 0"
                );
                log.info("✅ like_count column added successfully");
            } else {
                log.info("✅ like_count column already exists");
            }
            
        } catch (Exception e) {
            log.error("❌ Error updating comments table: {}", e.getMessage(), e);
        }
    }
    
    private void createCommentLikeTable() {
        try {
            log.info("🔧 Checking comment_likes table...");
            
            // 테이블 존재 여부 확인
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES " +
                "WHERE TABLE_SCHEMA = 'melodical_db' " +
                "AND TABLE_NAME = 'comment_likes'",
                Integer.class
            );
            
            if (count != null && count > 0) {
                log.info("✅ comment_likes table already exists");
                return;
            }
            
            log.info("🔄 Creating comment_likes table...");
            
            jdbcTemplate.execute(
                "CREATE TABLE comment_likes (" +
                "  id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "  user_id BIGINT NOT NULL," +
                "  comment_id BIGINT NOT NULL," +
                "  created_at DATETIME NOT NULL," +
                "  UNIQUE KEY uk_user_comment (user_id, comment_id)," +
                "  KEY idx_comment_id (comment_id)," +
                "  KEY idx_user_id (user_id)," +
                "  CONSTRAINT fk_comment_like_user FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE," +
                "  CONSTRAINT fk_comment_like_comment FOREIGN KEY (comment_id) REFERENCES comment(id) ON DELETE CASCADE" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci"
            );
            
            log.info("✅ comment_likes table created successfully");
            
        } catch (Exception e) {
            log.error("❌ Error creating comment_likes table: {}", e.getMessage(), e);
        }
    }
}
