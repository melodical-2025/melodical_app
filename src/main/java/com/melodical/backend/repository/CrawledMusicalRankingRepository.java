package com.melodical.backend.repository;

import com.melodical.backend.entity.CrawledMusicalRanking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CrawledMusicalRankingRepository extends JpaRepository<CrawledMusicalRanking, Long> {

    /**
     * 특정 타입(주간/월간)의 최신 데이터 조회
     */
    @Query("SELECT c FROM CrawledMusicalRanking c WHERE c.rankingType = :type " +
           "AND c.dataVersion = (SELECT MAX(c2.dataVersion) FROM CrawledMusicalRanking c2 WHERE c2.rankingType = :type) " +
           "ORDER BY c.combinedRank")
    List<CrawledMusicalRanking> findLatestByRankingType(@Param("type") String type);

    /**
     * 특정 데이터 버전의 모든 데이터 조회
     */
    List<CrawledMusicalRanking> findByDataVersionOrderByCombinedRank(String dataVersion);

    /**
     * 특정 타입과 버전의 데이터 조회
     */
    List<CrawledMusicalRanking> findByRankingTypeAndDataVersionOrderByCombinedRank(
            String rankingType, String dataVersion);

    /**
     * 정규화된 제목으로 검색 (최신 데이터)
     */
    @Query("SELECT c FROM CrawledMusicalRanking c WHERE c.normalizedTitle = :normalizedTitle " +
           "AND c.dataVersion = (SELECT MAX(c2.dataVersion) FROM CrawledMusicalRanking c2) " +
           "ORDER BY c.crawledAt DESC")
    Optional<CrawledMusicalRanking> findByNormalizedTitleLatest(@Param("normalizedTitle") String normalizedTitle);

    /**
     * 최신 데이터 버전 조회
     */
    @Query("SELECT MAX(c.dataVersion) FROM CrawledMusicalRanking c WHERE c.rankingType = :type")
    Optional<String> findLatestDataVersion(@Param("type") String type);

    /**
     * 특정 순위 범위의 데이터 조회
     */
    @Query("SELECT c FROM CrawledMusicalRanking c WHERE c.rankingType = :type " +
           "AND c.dataVersion = (SELECT MAX(c2.dataVersion) FROM CrawledMusicalRanking c2 WHERE c2.rankingType = :type) " +
           "AND c.combinedRank BETWEEN :startRank AND :endRank " +
           "ORDER BY c.combinedRank")
    List<CrawledMusicalRanking> findByRankingTypeAndRankRange(
            @Param("type") String type,
            @Param("startRank") int startRank,
            @Param("endRank") int endRank);

    /**
     * 오래된 데이터 버전 삭제용 (최신 N개 버전 제외)
     */
    @Query("SELECT DISTINCT c.dataVersion FROM CrawledMusicalRanking c ORDER BY c.dataVersion DESC")
    List<String> findAllDataVersions();

    /**
     * 특정 데이터 버전 삭제
     */
    void deleteByDataVersion(String dataVersion);

    /**
     * 예매 가능한 뮤지컬 조회
     */
    @Query("SELECT c FROM CrawledMusicalRanking c WHERE c.rankingType = :type " +
           "AND c.dataVersion = (SELECT MAX(c2.dataVersion) FROM CrawledMusicalRanking c2 WHERE c2.rankingType = :type) " +
           "AND c.isAvailable = true " +
           "ORDER BY c.combinedRank")
    List<CrawledMusicalRanking> findAvailableByRankingType(@Param("type") String type);
    
    /**
     * Interpark ID로 최신 크롤링 데이터 조회 (평점, URL 정보 제공용)
     */
    @Query("SELECT c FROM CrawledMusicalRanking c WHERE c.interparkId = :interparkId " +
           "ORDER BY c.crawledAt DESC")
    List<CrawledMusicalRanking> findByInterparkId(@Param("interparkId") String interparkId);
}

