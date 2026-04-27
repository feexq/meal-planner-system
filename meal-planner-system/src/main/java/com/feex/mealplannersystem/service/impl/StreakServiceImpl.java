package com.feex.mealplannersystem.service.impl;

import com.feex.mealplannersystem.common.user.StreakType;
import com.feex.mealplannersystem.repository.UserStreakMetaRepository;
import com.feex.mealplannersystem.repository.entity.auth.UserEntity;
import com.feex.mealplannersystem.repository.entity.profile.DailyNutritionSummaryEntity;
import com.feex.mealplannersystem.repository.entity.profile.UserStreakMetaEntity;
import com.feex.mealplannersystem.service.StreakService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StreakServiceImpl implements StreakService {

    private static final int STRICT_COMPLETION_THRESHOLD = 80;

    private final UserStreakMetaRepository streakMetaRepository;

    @Transactional
    public UserStreakMetaEntity evaluate(
            UserEntity user,
            DailyNutritionSummaryEntity summary,
            LocalDate today
    ) {
        UserStreakMetaEntity meta = getOrCreate(user);

        boolean dayCompleted = isDayCompleted(meta.getStreakType(), summary);
        if (!dayCompleted) {
            log.debug("Streak not updated for user={} date={}: day not completed", user.getId(), today);
            return meta;
        }

        LocalDate lastActive = meta.getLastActiveDate();

        if (today.equals(lastActive)) {
            return meta;
        }

        if (lastActive != null && lastActive.equals(today.minusDays(1))) {
            incrementStreak(meta, today);
            log.info("Streak incremented for user={}: currentStreak={}", user.getId(), meta.getCurrentStreak());
            return streakMetaRepository.save(meta);
        }

        if (lastActive != null && hasGap(lastActive, today)) {
            if (canUseFreeze(meta, lastActive, today)) {
                applyFreeze(meta, today);
                incrementStreak(meta, today);
                log.info("Freeze used for user={}: streak preserved at {}", user.getId(), meta.getCurrentStreak());
            } else {
                log.info("Streak reset for user={}: gap too large, no freezes", user.getId());
                resetStreak(meta, today);
            }
            return streakMetaRepository.save(meta);
        }
        startStreak(meta, today);
        log.info("Streak started for user={}", user.getId());
        return streakMetaRepository.save(meta);
    }

    @Transactional(readOnly = true)
    public UserStreakMetaEntity getStreak(Long userId) {
        return streakMetaRepository.findByUserId(userId)
                .orElseGet(() -> UserStreakMetaEntity.builder().build());
    }

    @Transactional
    public void changeStreakType(UserEntity user, StreakType newType) {
        UserStreakMetaEntity meta = getOrCreate(user);
        meta.setStreakType(newType);
        streakMetaRepository.save(meta);
        log.info("Streak type changed for user={}: {}", user.getId(), newType);
    }

    @Scheduled(cron = "0 5 0 * * *")
    @Transactional
    public void resetExpiredStreaks() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        List<UserStreakMetaEntity> expired = streakMetaRepository.findExpiredStreaks(yesterday);

        for (UserStreakMetaEntity meta : expired) {
            if (meta.getFreezesAvailable() > 0 &&
                    meta.getLastActiveDate() != null &&
                    meta.getLastActiveDate().equals(LocalDate.now().minusDays(2))) {

                applyFreeze(meta, yesterday);
                log.info("Auto-freeze applied for user={}", meta.getUser().getId());
            } else {
                meta.setCurrentStreak(0);
                log.info("Streak auto-reset for user={}", meta.getUser().getId());
            }
        }

        if (!expired.isEmpty()) {
            streakMetaRepository.saveAll(expired);
            log.info("Processed {} expired streaks", expired.size());
        }
    }

    @Scheduled(cron = "0 1 0 1 * *")
    @Transactional
    public void refillMonthlyFreezes() {
        streakMetaRepository.refillMonthlyFreezes();
        log.info("Monthly freezes refilled for all users");
    }

    private UserStreakMetaEntity getOrCreate(UserEntity user) {
        return streakMetaRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    UserStreakMetaEntity meta = UserStreakMetaEntity.builder()
                            .user(user)
                            .build();
                    return streakMetaRepository.save(meta);
                });
    }

    private boolean isDayCompleted(StreakType type, DailyNutritionSummaryEntity summary) {
        return switch (type) {
            case STRICT -> summary.getCompletionRate() >= STRICT_COMPLETION_THRESHOLD;
            case CASUAL -> summary.getTotalCalories() > 0;
        };
    }

    private boolean hasGap(LocalDate lastActive, LocalDate today) {
        return lastActive.isBefore(today.minusDays(1));
    }

    private boolean canUseFreeze(UserStreakMetaEntity meta, LocalDate lastActive, LocalDate today) {
        boolean hasFreeze = meta.getFreezesAvailable() > 0;
        boolean singleDayGap = lastActive.equals(today.minusDays(2));
        boolean notAlreadyUsed = meta.getLastFreezeUsedDate() == null ||
                !meta.getLastFreezeUsedDate().equals(today.minusDays(1));
        return hasFreeze && singleDayGap && notAlreadyUsed;
    }

    private void incrementStreak(UserStreakMetaEntity meta, LocalDate today) {
        int newStreak = meta.getCurrentStreak() + 1;
        meta.setCurrentStreak(newStreak);
        meta.setLastActiveDate(today);
        meta.setTotalActiveDays(meta.getTotalActiveDays() + 1);

        if (newStreak > meta.getLongestStreak()) {
            meta.setLongestStreak(newStreak);
        }
    }

    private void startStreak(UserStreakMetaEntity meta, LocalDate today) {
        meta.setCurrentStreak(1);
        meta.setLastActiveDate(today);
        meta.setTotalActiveDays(meta.getTotalActiveDays() + 1);

        if (meta.getLongestStreak() < 1) {
            meta.setLongestStreak(1);
        }
    }

    private void resetStreak(UserStreakMetaEntity meta, LocalDate today) {
        meta.setCurrentStreak(1); // новий день вже є активним
        meta.setLastActiveDate(today);
        meta.setTotalActiveDays(meta.getTotalActiveDays() + 1);
    }

    private void applyFreeze(UserStreakMetaEntity meta, LocalDate today) {
        meta.setFreezesAvailable(meta.getFreezesAvailable() - 1);
        meta.setFreezesUsedThisMonth(meta.getFreezesUsedThisMonth() + 1);
        meta.setLastFreezeUsedDate(today.minusDays(1)); // пропущений день
    }
}