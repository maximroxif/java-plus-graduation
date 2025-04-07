package ru.practicum.ewm.category.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.ewm.category.dto.CategoryDto;
import ru.practicum.ewm.category.dto.NewCategoryDto;
import ru.practicum.ewm.category.model.Category;


@UtilityClass
public class CategoryMapper {
    public static CategoryDto toCategoryDto(Category category) {
        return new CategoryDto(
                category.getId(),
                category.getName()
        );
    }


    public static Category dtoToCategory(NewCategoryDto newCategoryDto) {

        Category category = new Category();
        category.setName(newCategoryDto.getName());
        return category;
    }
}