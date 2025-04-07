package ru.practicum.ewm.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class NewCategoryDto {
    @Size(min = 1, max = 50, message = "Имя не более 50 символов")
    @NotBlank
    String name;
}
