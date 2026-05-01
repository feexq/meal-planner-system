package com.feex.mealplannersystem.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface ImageUploadService {
    String uploadImage(MultipartFile file, String fileName, String containerName) throws IOException;
    void deleteImage(String imageUrl, String containerName);
}
