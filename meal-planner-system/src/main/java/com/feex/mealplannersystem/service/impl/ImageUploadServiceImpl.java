package com.feex.mealplannersystem.service.impl;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.feex.mealplannersystem.service.ImageUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageUploadServiceImpl implements ImageUploadService {

    private static final long MAX_IMAGE_SIZE_BYTES = 5 * 1024 * 1024;
    private static final List<String> ALLOWED_CONTENT_TYPES =
            List.of("image/jpeg", "image/png", "image/webp");

    private final BlobServiceClient blobServiceClient;

    @Override
    public String uploadImage(MultipartFile file, String fileName, String containerName) throws IOException {
        validateImageFile(file);

        String extension = getExtension(file.getOriginalFilename());
        String blobName = fileName + "." + extension;

        BlobContainerClient container = blobServiceClient.getBlobContainerClient(containerName);
        BlobClient blobClient = container.getBlobClient(blobName);

        BlobHttpHeaders headers = new BlobHttpHeaders().setContentType(file.getContentType());

        blobClient.upload(file.getInputStream(), file.getSize(), true);
        blobClient.setHttpHeaders(headers);

        String imageUrl = blobClient.getBlobUrl();
        log.info("Image uploaded successfully: {}", imageUrl);

        return imageUrl;
    }

    @Override
    public void deleteImage(String imageUrl, String containerName) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return;
        }

        try {
            String cleanUrl = imageUrl;
            int queryIndex = cleanUrl.indexOf('?');
            if (queryIndex != -1) {
                cleanUrl = cleanUrl.substring(0, queryIndex);
            }

            cleanUrl = java.net.URLDecoder.decode(cleanUrl, java.nio.charset.StandardCharsets.UTF_8);

            int containerIndex = cleanUrl.indexOf(containerName + "/");
            if (containerIndex == -1) {
                log.warn("Invalid image URL format for deletion: {}", imageUrl);
                return;
            }

            String blobName = cleanUrl.substring(containerIndex + containerName.length() + 1);

            BlobContainerClient container = blobServiceClient.getBlobContainerClient(containerName);
            boolean isDeleted = container.getBlobClient(blobName).deleteIfExists();

            if (isDeleted) {
                log.info("Image successfully deleted from Azure: {}", blobName);
            } else {
                log.warn("Image not found in Azure for deletion: {}", blobName);
            }

        } catch (Exception e) {
            log.error("Error while deleting image: {}. Reason: {}", imageUrl, e.getMessage(), e);
        }
    }

    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        if (file.getSize() > MAX_IMAGE_SIZE_BYTES) {
            throw new IllegalArgumentException("File too large. Max 5MB allowed");
        }
        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException("Invalid file type. Allowed: JPEG, PNG, WebP");
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "jpg";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
