package com.feex.mealplannersystem.repository;

import com.feex.mealplannersystem.repository.entity.profile.UserStreakMetaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserStreakMetaRepository extends JpaRepository<UserStreakMetaEntity, Long> {

    Optional<UserStreakMetaEntity> findByUserId(Long userId);

    @Modifying
    @Query("UPDATE UserStreakMetaEntity s SET " +
            "s.freezesAvailable = s.freezesAvailable + 2, " +
            "s.freezesUsedThisMonth = 0 " +
            "WHERE s.freezesAvailable < 5")
    void refillMonthlyFreezes();

    @Query("SELECT s FROM UserStreakMetaEntity s WHERE " +
            "s.currentStreak > 0 AND " +
            "s.lastActiveDate < :yesterday AND " +
            "(s.lastFreezeUsedDate IS NULL OR s.lastFreezeUsedDate < :yesterday)")
    List<UserStreakMetaEntity> findExpiredStreaks(@Param("yesterday") LocalDate yesterday);
}