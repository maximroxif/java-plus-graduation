package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.dto.category.NewCategoryDto;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.CategoryMapper;
import ru.practicum.model.Category;
import ru.practicum.repository.CategoryRepository;

import jakarta.validation.Valid;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Validated
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public void delete(long categoryId) {
        log.info("Deleting category with id={}", categoryId);

        Category category = findCategoryById(categoryId);
        categoryRepository.delete(category);
        log.info("Category id={} successfully deleted", categoryId);
    }

    @Override
    @Transactional
    public CategoryDto createCategory(@Valid NewCategoryDto dto) {
        log.info("Creating new category: {}", dto);

        if (categoryRepository.existsByName(dto.getName())) {
            log.error("Category with name={} already exists", dto.getName());
            throw new ConflictException("Category with name=" + dto.getName() + " already exists");
        }

        Category category = CategoryMapper.dtoToCategory(dto);
        Category savedCategory = categoryRepository.save(category);
        log.info("Category successfully created with id={}", savedCategory.getId());

        return CategoryMapper.toCategoryDto(savedCategory);
    }

    @Override
    @Transactional
    public CategoryDto updateCategory(@Valid CategoryDto dto) {
        log.info("Updating category with id={}: {}", dto.getId(), dto);

        Category category = findCategoryById(dto.getId());

        if (!category.getName().equals(dto.getName()) && categoryRepository.existsByName(dto.getName())) {
            log.error("Category with name={} already exists", dto.getName());
            throw new ConflictException("Category with name=" + dto.getName() + " already exists");
        }

        category.setName(dto.getName());
        Category updatedCategory = categoryRepository.save(category);
        log.info("Category id={} successfully updated", dto.getId());

        return CategoryMapper.toCategoryDto(updatedCategory);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryDto> getAll(int from, int size) {
        log.info("Fetching categories with from={}, size={}", from, size);

        if (size <= 0) {
            log.warn("Invalid size value: {}, returning empty list", size);
            return List.of();
        }

        Pageable pageable = PageRequest.of(from > 0 ? from / size : 0, size);
        List<Category> categories = categoryRepository.findAll(pageable).getContent();

        log.info("Found {} categories", categories.size());
        return categories.stream()
                .map(CategoryMapper::toCategoryDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryDto getById(Long categoryId) {
        log.info("Fetching category with id={}", categoryId);

        if (categoryId == null) {
            log.error("CategoryId is null");
            throw new IllegalArgumentException("CategoryId cannot be null");
        }

        Category category = findCategoryById(categoryId);
        log.debug("Category id={} found", categoryId);

        return CategoryMapper.toCategoryDto(category);
    }

    private Category findCategoryById(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> {
                    log.error("Category with id={} not found", categoryId);
                    return new NotFoundException("Category with id=" + categoryId + " not found");
                });
    }
}