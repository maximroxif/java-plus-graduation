package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.dto.category.NewCategoryDto;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.CategoryMapper;
import ru.practicum.model.Category;
import ru.practicum.repository.CategoryRepository;

import java.util.List;

import static ru.practicum.mapper.CategoryMapper.dtoToCategory;
import static ru.practicum.mapper.CategoryMapper.toCategoryDto;

@Slf4j
@Service
@RequiredArgsConstructor
@Validated
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Override
    public CategoryDto createCategory(NewCategoryDto dto) {
        log.info("Creating new category with name={}", dto.getName());

        Category category = dtoToCategory(dto);
        log.debug("Mapped NewCategoryDto to Category: {}", category);

        Category savedCategory = categoryRepository.save(category);
        log.info("Category created successfully: id={}, name={}", savedCategory.getId(), savedCategory.getName());

        return toCategoryDto(savedCategory);
    }

    @Override
    public CategoryDto updateCategory(CategoryDto dto) {
        log.info("Updating category with id={}", dto.getId());

        Category category = findCategoryById(dto.getId());
        log.debug("Retrieved category: id={}, name={}", category.getId(), category.getName());

        category.setName(dto.getName());
        log.debug("Updated category name to: {}", dto.getName());

        Category updatedCategory = categoryRepository.save(category);
        log.info("Category updated successfully: id={}, name={}", updatedCategory.getId(), updatedCategory.getName());

        return toCategoryDto(updatedCategory);
    }

    @Override
    public void delete(long categoryId) {
        log.info("Deleting category with id={}", categoryId);

        Category category = findCategoryById(categoryId);
        log.debug("Category found for deletion: id={}, name={}", category.getId(), category.getName());

        categoryRepository.deleteById(categoryId);
        log.info("Category deleted successfully: id={}", categoryId);
    }

    @Override
    public List<CategoryDto> getAll(int from, int size) {
        log.info("Fetching categories with pagination: from={}, size={}", from, size);

        Pageable pageable = PageRequest.of(from > 0 ? from / size : 0, size);
        log.debug("Created pageable: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());

        Page<Category> categoriesPage = categoryRepository.findAll(pageable);
        log.debug("Retrieved {} categories", categoriesPage.getContent().size());

        List<CategoryDto> categoryDtos = categoriesPage.getContent().stream()
                .map(CategoryMapper::toCategoryDto)
                .toList();
        log.info("Returning {} categories", categoryDtos.size());

        return categoryDtos;
    }

    @Override
    public CategoryDto getById(Long categoryId) {
        log.info("Fetching category with id={}", categoryId);

        Category category = findCategoryById(categoryId);
        log.debug("Retrieved category: id={}, name={}", category.getId(), category.getName());

        CategoryDto categoryDto = toCategoryDto(category);
        log.info("Returning category: id={}, name={}", categoryDto.getId(), categoryDto.getName());

        return categoryDto;
    }

    /**
     * Helper method to find a category by ID, throwing NotFoundException if not found.
     *
     * @param categoryId the ID of the category
     * @return the found Category
     * @throws NotFoundException if the category is not found
     */
    private Category findCategoryById(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> {
                    log.error("Category with id={} not found", categoryId);
                    return new NotFoundException(String.format("Category with id=%d not found", categoryId));
                });
    }
}