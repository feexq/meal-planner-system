package com.feex.mealplannersystem.service.impl;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.feex.mealplannersystem.dto.profile.ProfileSummaryResponse;
import com.feex.mealplannersystem.dto.profile.UpdateProfileRequest;
import com.feex.mealplannersystem.dto.profile.statistic.WeeklyAveragesResponse;
import com.feex.mealplannersystem.repository.UserProfileRepository;
import com.feex.mealplannersystem.repository.UserPreferenceRepository;
import com.feex.mealplannersystem.repository.entity.auth.UserEntity;
import com.feex.mealplannersystem.repository.entity.preference.UserPreferenceEntity;
import com.feex.mealplannersystem.repository.entity.profile.UserProfileEntity;
import com.feex.mealplannersystem.repository.entity.profile.UserStreakMetaEntity;
import com.feex.mealplannersystem.repository.entity.profile.WeightHistoryEntity;
import com.feex.mealplannersystem.service.DailyNutritionSummaryService;
import com.feex.mealplannersystem.service.StreakService;
import com.feex.mealplannersystem.service.UserProfileService;
import com.feex.mealplannersystem.service.WeightService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private static final long MAX_AVATAR_SIZE_BYTES = 5 * 1024 * 1024;
    private static final List<String> ALLOWED_CONTENT_TYPES =
            List.of("image/jpeg", "image/png", "image/webp");

    private final UserProfileRepository profileRepository;
    private final UserPreferenceRepository preferenceRepository;
    private final WeightService weightService;
    private final StreakService streakService;
    private final DailyNutritionSummaryService summaryService;
    private final BlobServiceClient blobServiceClient;

    @Value("${spring.azure.storage.profile_container}")
    private String avatarContainer;

    @Transactional
    public UserProfileEntity initProfile(UserEntity user) {
        if (profileRepository.existsByUserId(user.getId())) {
            return profileRepository.findByUserId(user.getId()).orElseThrow();
        }
        UserProfileEntity profile = UserProfileEntity.builder()
                .user(user)
                .timezone("UTC")
                .build();
        return profileRepository.save(profile);
    }

    @Transactional(readOnly = true)
    public ProfileSummaryResponse getProfileSummary(UserEntity user) {
        UserProfileEntity profile = profileRepository.findByUserId(user.getId())
                .orElseGet(() -> UserProfileEntity.builder().user(user).build());

        Optional<UserPreferenceEntity> preferences = preferenceRepository.findByUserId(user.getId());

        UserStreakMetaEntity streak = streakService.getStreak(user.getId());

        Optional<WeightHistoryEntity> latestWeight =
                weightService.getLatestWeight(user.getId());

        List<WeightHistoryEntity> weightHistory =
                weightService.getRecentHistory(user.getId(), 8);

        LocalDate weekStart = LocalDate.now().with(
                java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        WeeklyAveragesResponse weeklyAverages =
                summaryService.getWeeklyAverages(user.getId(), weekStart);

        Double targetWeightKg = preferences.map(UserPreferenceEntity::getTargetWeightKg).orElse(null);

        Double weightProgressPercent = null;
        if (latestWeight.isPresent() && targetWeightKg != null) {
            double startWeight = weightHistory.isEmpty()
                    ? latestWeight.get().getWeightKg()
                    : weightHistory.get(weightHistory.size() - 1).getWeightKg();

            weightProgressPercent = weightService.calculateProgressPercent(
                    startWeight,
                    targetWeightKg,
                    latestWeight.get().getWeightKg()
            );
        }

        return ProfileSummaryResponse.builder()
                .userId(user.getId().toString())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .avatarUrl(profile.getAvatarUrl())
                .timezone(profile.getTimezone())
                .bio(profile.getBio())
                .age(calculateAge(profile.getDateOfBirth()))
                .currentWeightKg(latestWeight.map(WeightHistoryEntity::getWeightKg).orElse(null))
                .targetWeightKg(targetWeightKg)
                .weightProgressPercent(weightProgressPercent)
                .currentStreak(streak.getCurrentStreak())
                .longestStreak(streak.getLongestStreak())
                .totalActiveDays(streak.getTotalActiveDays())
                .freezesAvailable(streak.getFreezesAvailable())
                .streakType(streak.getStreakType())
                .weeklyAverages(weeklyAverages)
                .build();
    }

    @Transactional
    public UserProfileEntity updateProfile(UserEntity user, UpdateProfileRequest request) {
        UserProfileEntity profile = profileRepository.findByUserId(user.getId())
                .orElseGet(() -> initProfile(user));

        if (request.getTimezone() != null) {
            validateTimezone(request.getTimezone());
            profile.setTimezone(request.getTimezone());
        }
        if (request.getDateOfBirth() != null) {
            profile.setDateOfBirth(request.getDateOfBirth());
        }
        if (request.getBio() != null) {
            profile.setBio(request.getBio().length() > 300
                    ? request.getBio().substring(0, 300)
                    : request.getBio());
        }

        if (request.getTargetWeightKg() != null) {
            preferenceRepository.findByUserId(user.getId()).ifPresent(pref -> {
                pref.setTargetWeightKg(request.getTargetWeightKg());
                preferenceRepository.save(pref);
            });
        }

        return profileRepository.save(profile);
    }

    @Transactional
    public String uploadAvatar(UserEntity user, MultipartFile file) throws IOException {
        validateAvatarFile(file);

        String extension = getExtension(file.getOriginalFilename());
        String blobName = "avatars/%s/%s.%s"
                .formatted(user.getId(), UUID.randomUUID(), extension);

        BlobContainerClient container = blobServiceClient
                .getBlobContainerClient(avatarContainer);

        BlobClient blobClient = container.getBlobClient(blobName);

        BlobHttpHeaders headers = new BlobHttpHeaders()
                .setContentType(file.getContentType());

        blobClient.upload(file.getInputStream(), file.getSize(), true);
        blobClient.setHttpHeaders(headers);

        String avatarUrl = blobClient.getBlobUrl();

        UserProfileEntity profile = profileRepository.findByUserId(user.getId())
                .orElseGet(() -> initProfile(user));

        deleteOldAvatar(profile.getAvatarUrl(), user.getId());
        profile.setAvatarUrl(avatarUrl);
        profileRepository.save(profile);

        log.info("Avatar uploaded for user={}: {}", user.getId(), avatarUrl);
        return avatarUrl;
    }

    @Transactional
    public void deleteAvatar(UserEntity user) {
        UserProfileEntity profile = profileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalStateException("Profile not found"));

        if (profile.getAvatarUrl() == null) return;

        deleteOldAvatar(profile.getAvatarUrl(), user.getId());
        profile.setAvatarUrl(null);
        profileRepository.save(profile);

        log.info("Avatar deleted for user={}", user.getId());
    }

    private void validateAvatarFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        if (file.getSize() > MAX_AVATAR_SIZE_BYTES) {
            throw new IllegalArgumentException("File too large. Max 5MB allowed");
        }
        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException(
                    "Invalid file type. Allowed: JPEG, PNG, WebP");
        }
    }

    private void validateTimezone(String timezone) {
        try {
            java.time.ZoneId.of(timezone);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid timezone: " + timezone);
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "jpg";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    private void deleteOldAvatar(String oldUrl, Long userId) {
        if (oldUrl == null) return;
        try {
            String cleanUrl = oldUrl;
            int queryIndex = cleanUrl.indexOf('?');
            if (queryIndex != -1) {
                cleanUrl = cleanUrl.substring(0, queryIndex);
            }

            cleanUrl = java.net.URLDecoder.decode(cleanUrl, java.nio.charset.StandardCharsets.UTF_8);

            int prefixIndex = cleanUrl.indexOf("avatars/");
            if (prefixIndex == -1) {
                log.warn("Invalid avatar URL format for user={}: {}", userId, oldUrl);
                return;
            }

            String blobName = cleanUrl.substring(prefixIndex);

            BlobContainerClient container = blobServiceClient.getBlobContainerClient(avatarContainer);

            boolean isDeleted = container.getBlobClient(blobName).deleteIfExists();

            if (isDeleted) {
                log.info("Old avatar successfully deleted from Azure: {}", blobName);
            } else {
                log.warn("Avatar not found in Azure for deletion (might be already deleted): {}", blobName);
            }

        } catch (Exception e) {
            log.error("Error while deleting old avatar for user={}. Reason: {}", userId, e.getMessage(), e);
        }
    }

    private Integer calculateAge(java.time.LocalDate dateOfBirth) {
        if (dateOfBirth == null) return null;
        return Period.between(dateOfBirth, LocalDate.now()).getYears();
    }
}