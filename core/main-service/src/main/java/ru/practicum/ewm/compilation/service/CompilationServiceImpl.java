package ru.practicum.ewm.compilation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.compilation.dto.*;
import ru.practicum.ewm.event.dto.EventShortDto;
import ru.practicum.ewm.compilation.model.Compilation;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.compilation.mapper.CompilationMapper;
import ru.practicum.ewm.event.mapper.EventMapper;
import ru.practicum.ewm.compilation.repository.CompilationRepository;
import ru.practicum.ewm.event.repository.EventRepository;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompilationServiceImpl implements CompilationService {

    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final CompilationMapper compilationMapper;
    private final EventMapper eventMapper;

    @Override
    public List<CompilationDto> getAll(Boolean pinned, int from, int size) {
        Pageable pageable = PageRequest.of(from / size, size);
        Page<Compilation> compilationsPage = (pinned != null)
                ? compilationRepository.findAllByPinned(pinned, pageable)
                : compilationRepository.findAll(pageable);

        return compilationsPage.stream()
                .map(this::mapToCompilationDto)
                .toList();
    }

    @Override
    public CompilationDto getById(Long id) {
        Compilation compilation = getCompilationById(id);
        return mapToCompilationDto(compilation);
    }

    @Override
    @Transactional
    public void delete(long compilationId) {
        if (!compilationRepository.existsById(compilationId)) {
            throw new NotFoundException("Compilation with id " + compilationId + " not found");
        }
        compilationRepository.deleteById(compilationId);
    }

    @Override
    @Transactional
    public CompilationDto createCompilation(NewCompilationDto dto) {
        List<Event> events = getEventsByIds(dto.getEvents());
        Compilation compilation = compilationMapper.toCompilation(dto, events);
        Compilation savedCompilation = compilationRepository.save(compilation);
        return mapToCompilationDto(savedCompilation);
    }

    @Override
    @Transactional
    public CompilationDto updateCompilation(Long id, UpdateCompilationRequestDto dto) {
        Compilation compilation = getCompilationById(id);

        if (dto.getPinned() != null) {
            compilation.setPinned(dto.getPinned());
        }

        if (dto.getTitle() != null && !dto.getTitle().isBlank()) {
            compilation.setTitle(dto.getTitle());
        }

        if (dto.getEvents() != null) {
            compilation.setEvents(getEventsByIds(dto.getEvents()));
        }

        Compilation updatedCompilation = compilationRepository.save(compilation);
        return mapToCompilationDto(updatedCompilation);
    }

    private Compilation getCompilationById(Long id) {
        return compilationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Compilation with id " + id + " not found"));
    }

    private List<Event> getEventsByIds(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<Event> events = eventRepository.findAllById(eventIds);

        if (events.size() != eventIds.size()) {
            throw new NotFoundException("Some events not found");
        }

        return events;
    }

    private CompilationDto mapToCompilationDto(Compilation compilation) {
        List<EventShortDto> eventDtos = compilation.getEvents().stream()
                .map(eventMapper::eventToEventShortDto)
                .toList();
        return compilationMapper.toCompilationDto(compilation, eventDtos);
    }
}