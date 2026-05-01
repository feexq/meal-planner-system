package com.feex.mealplannersystem.service;

import com.feex.mealplannersystem.domain.category.Category;
import com.feex.mealplannersystem.dto.category.CreateCategoryRequest;
import com.feex.mealplannersystem.dto.category.UpdateCategoryRequest;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface CategoryService {
    List<Category> getAllRoots();       // всі кореневі з дітьми
    List<Category> getAll();           // всі плоским списком
    Category getById(Long id);
    Category getBySlug(String slug);
    Category create(CreateCategoryRequest request);
    Category update(Long id, UpdateCategoryRequest request);
    void delete(Long id);
    String uploadImage(Long id, MultipartFile file) throws IOException;
    void deleteImage(Long id);
}
