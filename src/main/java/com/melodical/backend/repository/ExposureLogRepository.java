package com.melodical.backend.repository;

import com.melodical.backend.entity.ExposureLog;
import com.melodical.backend.entity.Musical;
import com.melodical.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ExposureLogRepository extends JpaRepository<ExposureLog, Long> {

    List<ExposureLog> findByUserAndExposedAtAfter(User user, LocalDateTime after);

    List<ExposureLog> findByUserAndMusicalAndExposedAtAfter(User user, Musical musical, LocalDateTime after);

    @Query("SELECT el FROM ExposureLog el WHERE el.user = :user " +
           "AND el.exposedAt >= :startTime ORDER BY el.exposedAt DESC")
    List<ExposureLog> findRecentExposures(@Param("user") User user,
                                         @Param("startTime") LocalDateTime startTime);

    @Query("SELECT el.musical, COUNT(el) FROM ExposureLog el " +
           "WHERE el.exposedAt >= :startTime GROUP BY el.musical " +
           "ORDER BY COUNT(el) DESC")
    List<Object[]> findMostExposedMusicals(@Param("startTime") LocalDateTime startTime);

    @Query("SELECT COUNT(el) FROM ExposureLog el WHERE el.user = :user " +
           "AND el.musical = :musical AND DATE(el.exposedAt) = DATE(:date)")
    Long countDailyExposures(@Param("user") User user,
                            @Param("musical") Musical musical,
                            @Param("date") LocalDateTime date);

    @Query("SELECT el.surface, COUNT(el) FROM ExposureLog el " +
           "WHERE el.exposedAt >= :startTime GROUP BY el.surface")
    List<Object[]> findExposuresBySurface(@Param("startTime") LocalDateTime startTime);
}
