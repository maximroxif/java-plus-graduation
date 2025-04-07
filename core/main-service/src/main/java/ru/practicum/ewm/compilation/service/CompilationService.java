package ru.practicum.ewm.compilation.service;

import ru.practicum.ewm.compilation.dto.CompilationDto;
import ru.practicum.ewm.compilation.dto.NewCompilationDto;
import ru.practicum.ewm.compilation.dto.UpdateCompilationRequestDto;

import java.util.List;

public interface CompilationService {

    List<CompilationDto> getAll(Boolean pinned, int from, int size);

    CompilationDto getById(Long categoryId);

    void delete(long compilationId);

    CompilationDto createCompilation(NewCompilationDto dto);

    CompilationDto updateCompilation(Long id, UpdateCompilationRequestDto dto);
}