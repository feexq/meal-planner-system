package com.feex.mealplannersystem.repository;

import com.feex.mealplannersystem.repository.entity.profile.UserProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfileEntity, Long> {

    Optional<UserProfileEntity> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    @Query("SELECT p FROM UserProfileEntity p JOIN FETCH p.user WHERE p.user.id = :userId")
    Optional<UserProfileEntity> findByUserIdWithUser(@Param("userId") Long userId);
}