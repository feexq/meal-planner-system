package com.feex.mealplannersystem.service.impl;

import com.feex.mealplannersystem.dto.user.UserPreferenceRequest;
import com.feex.mealplannersystem.dto.user.UserPreferenceResponse;
import com.feex.mealplannersystem.repository.UserPreferenceRepository;
import com.feex.mealplannersystem.repository.UserRepository;
import com.feex.mealplannersystem.repository.entity.preference.UserPreferenceEntity;
import com.feex.mealplannersystem.repository.entity.auth.UserEntity;
import com.feex.mealplannersystem.service.UserPreferenceService;
import com.feex.mealplannersystem.service.exception.CustomAlreadyExistsException;
import com.feex.mealplannersystem.service.exception.CustomNotFoundException;
import com.feex.mealplannersystem.service.mapper.UserPreferenceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserPreferenceServiceImpl implements UserPreferenceService {

    private final UserPreferenceRepository userPreferenceRepository;
    private final UserRepository userRepository;
    private final UserPreferenceMapper userPreferenceMapper;

    @Override
    @Transactional
    public UserPreferenceResponse submitPreference(String email, UserPreferenceRequest request) {
        if (userPreferenceRepository.existsByUserEmail(email)) {
            throw new CustomAlreadyExistsException("Preference", email);
        }

        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomNotFoundException("User" , email));

        UserPreferenceEntity entity = userPreferenceMapper.toEntity(request);
        entity.setUser(user);
        log.info(entity.toString());
        return userPreferenceMapper.toResponse(userPreferenceRepository.save(entity));
    }

    @Override
    @Transactional
    public UserPreferenceResponse updatePreference(String email, UserPreferenceRequest request) {
        UserPreferenceEntity entity = userPreferenceRepository.findByUserEmail(email)
                .orElseThrow(() -> new CustomNotFoundException("Preference", email));

        UserPreferenceEntity updated = userPreferenceMapper.updateEntity(entity, request);

        return userPreferenceMapper.toResponse(userPreferenceRepository.save(updated));
    }

    @Override
    @Transactional(readOnly = true)
    public UserPreferenceResponse getPreference(String email) {
        return userPreferenceRepository.findByUserEmail(email)
                .map(userPreferenceMapper::toResponse)
                .orElseThrow(() -> new CustomNotFoundException("Preference", email));
    }

    @Override
    @Transactional
    public void deletePreference(String email) {
        userPreferenceRepository.deleteByUserEmail(email);
    }

    @Override
    @Transactional
    public boolean hasPreference(String email) {
        return userPreferenceRepository.existsByUserEmail(email);
    }
}