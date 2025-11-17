package com.melodical.backend.repository;

import com.melodical.backend.entity.Musical;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MusicalRepository extends JpaRepository<Musical, Long> {

    /**
     * 월간 인기 뮤지컬 (CrawledMusicalRanking MONTHLY 타입 기준)
     * interparkId 기반으로 중복 제거
     */
    @Query(value = """
        SELECT m.* 
        FROM musical m
        INNER JOIN (
            SELECT c.interpark_id, MIN(c.combined_rank) as min_rank
            FROM crawled_musical_ranking c
            WHERE c.ranking_type = 'MONTHLY'
            AND c.data_version = (
                SELECT MAX(data_version) 
                FROM crawled_musical_ranking 
                WHERE ranking_type = 'MONTHLY'
            )
            AND c.interpark_id IS NOT NULL
            AND c.interpark_id != ''
            GROUP BY c.interpark_id
        ) ranked ON m.interpark_id = ranked.interpark_id
        ORDER BY ranked.min_rank ASC
        LIMIT 100
        """, nativeQuery = true)
    List<Musical> findMonthlyMusicals();

    /**
     * 주간 인기 뮤지컬 (CrawledMusicalRanking WEEKLY 타입 기준)
     * interparkId 기반으로 중복 제거
     */
    @Query(value = """
        SELECT m.* 
        FROM musical m
        INNER JOIN (
            SELECT c.interpark_id, MIN(c.combined_rank) as min_rank
            FROM crawled_musical_ranking c
            WHERE c.ranking_type = 'WEEKLY'
            AND c.data_version = (
                SELECT MAX(data_version) 
                FROM crawled_musical_ranking 
                WHERE ranking_type = 'WEEKLY'
            )
            AND c.interpark_id IS NOT NULL
            AND c.interpark_id != ''
            GROUP BY c.interpark_id
        ) ranked ON m.interpark_id = ranked.interpark_id
        ORDER BY ranked.min_rank ASC
        LIMIT 100
        """, nativeQuery = true)
    List<Musical> findWeeklyMusicals();

    /**
     * 월간 인기 Top N
     * interparkId 기반으로 중복 제거
     */
    @Query(value = """
        SELECT m.* 
        FROM musical m
        INNER JOIN (
            SELECT c.interpark_id, MIN(c.combined_rank) as min_rank
            FROM crawled_musical_ranking c
            WHERE c.ranking_type = 'MONTHLY'
            AND c.data_version = (
                SELECT MAX(data_version) 
                FROM crawled_musical_ranking 
                WHERE ranking_type = 'MONTHLY'
            )
            AND c.interpark_id IS NOT NULL
            AND c.interpark_id != ''
            GROUP BY c.interpark_id
        ) ranked ON m.interpark_id = ranked.interpark_id
        ORDER BY ranked.min_rank ASC
        LIMIT :count
        """, nativeQuery = true)
    List<Musical> findTopMonthlyMusicals(@Param("count") int count);

    /**
     * 제목으로 검색 (대소문자 무시, 공백 무시하여 검색)
     */
    @Query(value = """
        SELECT * FROM musical m 
        WHERE LOWER(REPLACE(m.title, ' ', '')) LIKE LOWER(CONCAT('%', REPLACE(:query, ' ', ''), '%'))
        ORDER BY m.popularity_score DESC, m.id DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Musical> searchByTitleContaining(@Param("query") String query, @Param("limit") int limit);
    
    /**
     * interparkId로 뮤지컬 조회 (가장 최근 것 하나만)
     */
    @Query(value = """
        SELECT * FROM musical m 
        WHERE m.interpark_id = :interparkId
        ORDER BY m.id DESC
        LIMIT 1
        """, nativeQuery = true)
    Musical findByInterparkId(@Param("interparkId") String interparkId);
    
    /**
     * interparkId로 모든 뮤지컬 조회 (중복 확인용)
     */
    List<Musical> findAllByInterparkId(String interparkId);
}
