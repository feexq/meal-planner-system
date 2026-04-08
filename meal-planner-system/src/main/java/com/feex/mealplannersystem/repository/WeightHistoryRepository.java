package com.feex.mealplannersystem.repository;

import com.feex.mealplannersystem.repository.entity.profile.WeightHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface WeightHistoryRepository extends JpaRepository<WeightHistoryEntity, Long> {

    @Query("SELECT w FROM WeightHistoryEntity w WHERE w.user.id = :userId " +
            "ORDER BY w.recordedDate DESC LIMIT 1")
    Optional<WeightHistoryEntity> findLatestByUserId(@Param("userId") Long userId);

    @Query("SELECT w FROM WeightHistoryEntity w WHERE w.user.id = :userId " +
            "AND w.recordedDate BETWEEN :from AND :to " +
            "ORDER BY w.recordedDate ASC")
    List<WeightHistoryEntity> findByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );

    @Query("SELECT w FROM WeightHistoryEntity w WHERE w.user.id = :userId " +
            "ORDER BY w.recordedDate DESC LIMIT :limit")
    List<WeightHistoryEntity> findRecentByUserId(
            @Param("userId") Long userId,
            @Param("limit") int limit
    );

    Optional<WeightHistoryEntity> findByUserIdAndRecordedDate(Long userId, LocalDate recordedDate);
}