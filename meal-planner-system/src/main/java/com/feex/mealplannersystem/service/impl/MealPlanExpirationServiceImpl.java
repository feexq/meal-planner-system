package com.feex.mealplannersystem.service.impl;

import com.feex.mealplannersystem.repository.MealPlanRecordRepository;
import com.feex.mealplannersystem.repository.entity.mealplan.MealPlanRecordEntity;
import com.feex.mealplannersystem.service.MealPlanExpirationService;
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
public class MealPlanExpirationServiceImpl implements MealPlanExpirationService {

    private final MealPlanRecordRepository planRepository;

    @Scheduled(cron = "0 5 0 * * *")
    @Transactional
    public void expireOldPlans() {
        LocalDate expirationThreshold = LocalDate.now().minusDays(7);

        List<MealPlanRecordEntity> expiredPlans = planRepository
                .findAllActiveOlderThan(expirationThreshold);

        if (expiredPlans.isEmpty()) return;

        for (MealPlanRecordEntity plan : expiredPlans) {
            plan.setStatus(MealPlanRecordEntity.MealPlanStatus.COMPLETED);
            log.info("Auto-completed plan {} for user {} (started {})",
                    plan.getId(), plan.getUser().getId(), plan.getWeekStartDate());
        }

        planRepository.saveAll(expiredPlans);
        log.info("Expired {} stale meal plans", expiredPlans.size());
    }
}
