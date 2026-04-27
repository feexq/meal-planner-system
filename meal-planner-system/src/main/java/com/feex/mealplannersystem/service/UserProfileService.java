package com.feex.mealplannersystem.service;

import com.feex.mealplannersystem.dto.profile.ProfileSummaryResponse;
import com.feex.mealplannersystem.dto.profile.UpdateProfileRequest;
import com.feex.mealplannersystem.repository.entity.auth.UserEntity;
import com.feex.mealplannersystem.repository.entity.profile.UserProfileEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface UserProfileService {

    UserProfileEntity initProfile(UserEntity user);

    ProfileSummaryResponse getProfileSummary(UserEntity user);

    UserProfileEntity updateProfile(UserEntity user, UpdateProfileRequest request);

    String uploadAvatar(UserEntity user, MultipartFile file) throws IOException;

    void deleteAvatar(UserEntity user);
}
