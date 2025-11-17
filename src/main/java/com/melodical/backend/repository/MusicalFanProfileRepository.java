package com.melodical.backend.repository;

import com.melodical.backend.entity.Musical;
import com.melodical.backend.entity.MusicalFanProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MusicalFanProfileRepository extends JpaRepository<MusicalFanProfile, Long> {

    List<MusicalFanProfile> findByMusical(Musical musical);

    Optional<MusicalFanProfile> findByMusicalAndGenreName(Musical musical, String genreName);

    @Query("SELECT mfp FROM MusicalFanProfile mfp WHERE mfp.musical = :musical " +
           "ORDER BY mfp.fanPreferenceScore DESC")
    List<MusicalFanProfile> findByMusicalOrderByFanPreferenceScoreDesc(@Param("musical") Musical musical);

    @Query("SELECT mfp FROM MusicalFanProfile mfp WHERE mfp.musical IN :musicals")
    List<MusicalFanProfile> findByMusicalIn(@Param("musicals") List<Musical> musicals);

    @Query("SELECT mfp.musical, AVG(mfp.fanPreferenceScore) FROM MusicalFanProfile mfp " +
           "WHERE mfp.genreName = :genreName GROUP BY mfp.musical")
    List<Object[]> findAverageScoreByGenre(@Param("genreName") String genreName);

    @Query("SELECT mfp FROM MusicalFanProfile mfp WHERE mfp.confidenceScore >= :minConfidence " +
           "ORDER BY mfp.fanPreferenceScore DESC")
    List<MusicalFanProfile> findHighConfidenceProfiles(@Param("minConfidence") Double minConfidence);
}
