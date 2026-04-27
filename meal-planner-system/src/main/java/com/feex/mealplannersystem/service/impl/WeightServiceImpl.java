package com.feex.mealplannersystem.service.impl;

import com.feex.mealplannersystem.repository.WeightHistoryRepository;
import com.feex.mealplannersystem.repository.entity.auth.UserEntity;
import com.feex.mealplannersystem.repository.entity.profile.WeightHistoryEntity;
import com.feex.mealplannersystem.service.WeightService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeightServiceImpl implements WeightService {

    private static final int DEFAULT_HISTORY_LIMIT = 30;

    private final WeightHistoryRepository weightHistoryRepository;

    @Transactional
    public WeightHistoryEntity logWeight(UserEntity user, double weightKg, String note) {
        LocalDate today = LocalDate.now();
        return logWeightForDate(user, weightKg, note, today);
    }

    @Transactional
    public WeightHistoryEntity logWeightForDate(
            UserEntity user,
            double weightKg,
            String note,
            LocalDate date
    ) {
        if (weightKg <= 0 || weightKg > 500) {
            throw new IllegalArgumentException("Invalid weight value: " + weightKg);
        }

        WeightHistoryEntity entry = weightHistoryRepository
                .findByUserIdAndRecordedDate(user.getId(), date)
                .orElseGet(() -> WeightHistoryEntity.builder()
                        .user(user)
                        .recordedDate(date)
                        .build());

        entry.setWeightKg(weightKg);
        entry.setNote(note);

        WeightHistoryEntity saved = weightHistoryRepository.save(entry);
        log.info("Weight logged for user={}: {}kg on {}", user.getId(), weightKg, date);
        return saved;
    }

    @Transactional(readOnly = true)
    public Optional<WeightHistoryEntity> getLatestWeight(Long userId) {
        return weightHistoryRepository.findLatestByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<WeightHistoryEntity> getRecentHistory(Long userId, int limit) {
        return weightHistoryRepository.findRecentByUserId(userId,
                limit > 0 ? limit : DEFAULT_HISTORY_LIMIT);
    }

    @Transactional(readOnly = true)
    public List<WeightHistoryEntity> getHistoryRange(Long userId, LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("'from' date must be before 'to' date");
        }
        return weightHistoryRepository.findByUserIdAndDateRange(userId, from, to);
    }

    public Double calculateProgressPercent(
            double startWeight,
            double targetWeight,
            double currentWeight
    ) {
        double totalChange = startWeight - targetWeight;
        if (Math.abs(totalChange) < 0.01) return null;

        double achieved = startWeight - currentWeight;

        double progress = (achieved / totalChange) * 100.0;

        return Math.min(Math.max(progress, 0.0), 100.0);
    }

    @Transactional
    public boolean deleteEntry(Long userId, LocalDate date) {
        Optional<WeightHistoryEntity> entry =
                weightHistoryRepository.findByUserIdAndRecordedDate(userId, date);

        if (entry.isEmpty()) return false;

        weightHistoryRepository.delete(entry.get());
        log.info("Weight entry deleted for user={} date={}", userId, date);
        return true;
    }
}