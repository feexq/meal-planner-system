package com.feex.mealplannersystem.service.impl;

import com.feex.mealplannersystem.common.AchievementType;
import com.feex.mealplannersystem.dto.profile.achievement.AchievementResponse;
import com.feex.mealplannersystem.dto.profile.achievement.CreateAchievementRequest;
import com.feex.mealplannersystem.dto.profile.achievement.UpdateAchievementRequest;
import com.feex.mealplannersystem.repository.AchievementRepository;
import com.feex.mealplannersystem.repository.MealPlanSlotRepository;
import com.feex.mealplannersystem.repository.UserAchievementRepository;
import com.feex.mealplannersystem.repository.UserPreferenceRepository;
import com.feex.mealplannersystem.repository.entity.auth.UserEntity;
import com.feex.mealplannersystem.repository.entity.preference.UserPreferenceEntity;
import com.feex.mealplannersystem.repository.entity.profile.AchievementEntity;
import com.feex.mealplannersystem.repository.entity.profile.UserAchievementEntity;
import com.feex.mealplannersystem.repository.entity.profile.UserStreakMetaEntity;
import com.feex.mealplannersystem.repository.entity.profile.WeightHistoryEntity;
import com.feex.mealplannersystem.service.AchievementService;
import com.feex.mealplannersystem.service.exception.CustomNotFoundException;
import com.feex.mealplannersystem.service.mapper.AchievementMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AchievementServiceImpl implements AchievementService{

    private final AchievementRepository achievementRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final MealPlanSlotRepository mealPlanSlotRepository;
    private final StreakService streakService;
    private final WeightServiceImpl weightService;
    private final UserPreferenceRepository preferenceRepository;
    private final AchievementMapper mapper;

    @Transactional
    public List<AchievementResponse> getUserAchievements(UserEntity user) {
        List<AchievementEntity> allAchievements = achievementRepository.findAll();
        Map<Long, UserAchievementEntity> userProgressMap = userAchievementRepository.findAllByUserId(user.getId())
                .stream()
                .collect(Collectors.toMap(ua -> ua.getAchievement().getId(), ua -> ua));

        UserStreakMetaEntity streak = streakService.getStreak(user.getId());

        Optional<WeightHistoryEntity> latestWeightOpt = weightService.getLatestWeight(user.getId());
        Optional<UserPreferenceEntity> preferenceOpt = preferenceRepository.findByUserId(user.getId());
        boolean targetWeightReached = false;
        if (latestWeightOpt.isPresent() && preferenceOpt.isPresent()) {
            Double target = preferenceOpt.get().getTargetWeightKg();
            Double current = latestWeightOpt.get().getWeightKg();
            if (target != null && current != null) {
                targetWeightReached = Math.abs(target - current) <= 0.5;
            }
        }
        
        Integer distinctRecipesEaten = mealPlanSlotRepository.countDistinctRecipesEatenByUserId(user.getId());

        boolean finalTargetWeightReached = targetWeightReached;
        return allAchievements.stream().map(achievement -> {
            UserAchievementEntity userAchievement = userProgressMap.get(achievement.getId());

            int progress = calculateProgress(achievement, streak, finalTargetWeightReached, distinctRecipesEaten);
            
            if (userAchievement == null) {
                userAchievement = UserAchievementEntity.builder()
                        .user(user)
                        .achievement(achievement)
                        .progress(progress)
                        .build();
            } else {
                userAchievement.setProgress(progress);
            }

            if (progress >= achievement.getTargetValue() && userAchievement.getAchievedAt() == null) {
                userAchievement.setAchievedAt(LocalDateTime.now());
                log.info("User {} achieved: {}", user.getId(), achievement.getKey());
            }

            userAchievementRepository.save(userAchievement);

            return mapper.toDto(achievement, userAchievement);
        }).collect(Collectors.toList());
    }

    private int calculateProgress(AchievementEntity achievement, UserStreakMetaEntity streak, boolean targetWeightReached, Integer distinctRecipesEaten) {
        return switch (achievement.getType()) {
            case STREAK -> streak.getLongestStreak() != null ? streak.getLongestStreak() : 0;
            case TOTAL_ACTIVE -> streak.getTotalActiveDays() != null ? streak.getTotalActiveDays() : 0;
            case WEIGHT -> targetWeightReached ? 1 : 0;
            case RECIPES -> distinctRecipesEaten != null ? distinctRecipesEaten : 0;
        };
    }

    @Transactional(readOnly = true)
    public List<AchievementEntity> getAllAchievements() {
        return achievementRepository.findAll();
    }

    @Transactional
    public AchievementEntity createAchievement(CreateAchievementRequest request) {
        AchievementEntity achievement = AchievementEntity.builder()
                .key(request.getKey())
                .title(request.getTitle())
                .description(request.getDescription())
                .iconUrl(request.getIconUrl())
                .targetValue(request.getTargetValue())
                .type(AchievementType.valueOf(request.getType().toUpperCase()))
                .build();
        return achievementRepository.save(achievement);
    }

    @Transactional
    public AchievementEntity updateAchievement(Long id, UpdateAchievementRequest request) {
        AchievementEntity achievement = achievementRepository.findById(id)
                .orElseThrow(() -> new CustomNotFoundException("Achievement", id.toString()));

        if (request.getTitle() != null) achievement.setTitle(request.getTitle());
        if (request.getDescription() != null) achievement.setDescription(request.getDescription());
        if (request.getIconUrl() != null) achievement.setIconUrl(request.getIconUrl());
        if (request.getTargetValue() != null) achievement.setTargetValue(request.getTargetValue());

        return achievementRepository.save(achievement);
    }

    @Transactional
    public void deleteAchievement(Long id) {
        if (!achievementRepository.existsById(id)) {
            throw new CustomNotFoundException("Achievement", id.toString());
        }
        achievementRepository.deleteById(id);
    }
}
