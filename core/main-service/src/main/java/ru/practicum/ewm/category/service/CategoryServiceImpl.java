package ru.practicum.ewm.category.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import ru.practicum.ewm.category.dto.CategoryDto;
import ru.practicum.ewm.category.dto.NewCategoryDto;
import ru.practicum.ewm.category.model.Category;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.category.mapper.CategoryMapper;
import ru.practicum.ewm.category.repository.CategoryRepository;
import ru.practicum.ewm.event.repository.EventRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository;

    @Override
    @Transactional
    public void delete(long categoryId) {
        Category category = getCategoryById(categoryId);

        if (eventRepository.existsByCategoryId(categoryId)) {
            throw new ConflictException("Cannot delete category with id " + categoryId +
                    " because it's used in events");
        }

        categoryRepository.delete(category);
    }

    @Override
    @Transactional
    public CategoryDto createCategory(@Validated NewCategoryDto dto) {
        validateCategoryNameUnique(dto.getName());
        Category category = CategoryMapper.dtoToCategory(dto);
        Category savedCategory = categoryRepository.save(category);
        return CategoryMapper.toCategoryDto(savedCategory);
    }

    @Override
    @Transactional
    public CategoryDto updateCategory(CategoryDto dto) {
        Category category = getCategoryById(dto.getId());

        if (!category.getName().equals(dto.getName())) {
            validateCategoryNameUnique(dto.getName());
        }

        category.setName(dto.getName());
        return CategoryMapper.toCategoryDto(category);
    }

    @Override
    public List<CategoryDto> getAll(int from, int size) {
        Pageable pageable = PageRequest.of(from / size, size);
        Page<Category> categoriesPage = categoryRepository.findAll(pageable);

        return categoriesPage.stream()
                .map(CategoryMapper::toCategoryDto)
                .toList();
    }

    @Override
    public CategoryDto getById(Long categoryId) {
        Category category = getCategoryById(categoryId);
        return CategoryMapper.toCategoryDto(category);
    }

    private Category getCategoryById(long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Category with id " + categoryId + " not found"));
    }

    private void validateCategoryNameUnique(String name) {
        if (categoryRepository.existsByName(name)) {
            throw new ConflictException("Category with name '" + name + "' already exists");
        }
    }
}