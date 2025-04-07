package ru.practicum.ewm.compilation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import ru.practicum.ewm.event.dto.EventShortDto;

import java.util.List;

@Data
@AllArgsConstructor
public class CompilationDto {
    Long id;
    List<EventShortDto> events;
    Boolean pinned;
    @NotBlank
    @Size(max = 50, message = "Имя не более 50 символов")
    @NotNull
    String title;
}