package com.feex.mealplannersystem.service;

import com.feex.mealplannersystem.repository.entity.auth.UserEntity;
import com.feex.mealplannersystem.repository.entity.profile.WeightHistoryEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WeightService {

    WeightHistoryEntity logWeight(UserEntity user, double weightKg, String note);

    WeightHistoryEntity logWeightForDate(UserEntity user, double weightKg, String note, LocalDate date);

    Optional<WeightHistoryEntity> getLatestWeight(Long userId);

    List<WeightHistoryEntity> getRecentHistory(Long userId, int limit);

    List<WeightHistoryEntity> getHistoryRange(Long userId, LocalDate from, LocalDate to);

    Double calculateProgressPercent(double startWeight, double targetWeight, double currentWeight);

    boolean deleteEntry(Long userId, LocalDate date);
}
