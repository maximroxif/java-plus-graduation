package ru.practicum.ewm.compilation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class NewCompilationDto {
    List<Long> events;
    boolean pinned;
    @NotBlank
    @Size(min = 1, max = 50, message = "Имя не более 50 символов")
    @NotNull
    String title;
}
