package ru.practicum.ewm.category.service;

import ru.practicum.ewm.category.dto.NewCategoryDto;
import ru.practicum.ewm.category.dto.CategoryDto;

import java.util.List;

public interface CategoryService {

    List<CategoryDto> getAll(int from, int size);

    CategoryDto getById(Long categoryId);

    void delete(long categoryId);

    CategoryDto createCategory(NewCategoryDto dto);

    CategoryDto updateCategory(CategoryDto dto);
}
