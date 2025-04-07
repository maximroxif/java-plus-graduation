package ru.practicum.ewm.service;

import ru.practicum.ewm.dto.category.NewCategoryDto;
import ru.practicum.ewm.dto.category.CategoryDto;

import java.util.List;

public interface CategoryService {

    List<CategoryDto> getAll(int from, int size);

    CategoryDto getById(Long categoryId);

    void delete(long categoryId);

    CategoryDto createCategory(NewCategoryDto dto);

    CategoryDto updateCategory(CategoryDto dto);
}
