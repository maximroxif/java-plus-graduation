package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.compilation.CompilationDto;
import ru.practicum.dto.compilation.NewCompilationDto;
import ru.practicum.dto.compilation.UpdateCompilationRequestDto;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.CompilationMapper;
import ru.practicum.mapper.EventMapper;
import ru.practicum.model.Compilation;
import ru.practicum.model.Event;
import ru.practicum.repository.CompilationRepository;
import ru.practicum.repository.EventRepository;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CompilationServiceImpl implements CompilationService {

    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final CompilationMapper compilationMapper;
    private final EventMapper eventMapper;

    @Override
    public List<CompilationDto> getAll(Boolean pinned, int from, int size) {
        log.info("Fetching compilations with pinned={}, from={}, size={}", pinned, from, size);

        if (size <= 0) {
            log.warn("Invalid size value: {}, returning empty list", size);
            return Collections.emptyList();
        }

        Pageable pageable = PageRequest.of(from > 0 ? from / size : 0, size);
        List<Compilation> compilations = (pinned != null)
                ? compilationRepository.findAllByPinned(pinned, pageable).getContent()
                : compilationRepository.findAll(pageable).getContent();

        log.info("Found {} compilations", compilations.size());
        return compilations.stream()
                .map(this::mapToCompilationDto)
                .toList();
    }

    @Override
    public CompilationDto getById(Long id) {
        log.info("Fetching compilation with id={}", id);

        if (id == null) {
            log.error("Compilation id is null");
            throw new IllegalArgumentException("Compilation id cannot be null");
        }

        Compilation compilation = findCompilationById(id);
        log.debug("Compilation id={} found", id);

        return mapToCompilationDto(compilation);
    }

    @Override
    @Transactional
    public void delete(long compilationId) {
        log.info("Deleting compilation with id={}", compilationId);

        findCompilationById(compilationId); // Проверка существования
        compilationRepository.deleteById(compilationId);
        log.info("Compilation id={} successfully deleted", compilationId);
    }

    @Override
    @Transactional
    public CompilationDto createCompilation(NewCompilationDto dto) {
        log.info("Creating new compilation: {}", dto);

        if (dto == null) {
            log.error("NewCompilationDto is null");
            throw new IllegalArgumentException("NewCompilationDto cannot be null");
        }

        List<Event> events = getEventsByIds(dto.getEvents());
        Compilation compilation = compilationMapper.toCompilation(dto, events);
        Compilation savedCompilation = compilationRepository.save(compilation);
        log.info("Compilation successfully created with id={}", savedCompilation.getId());

        return mapToCompilationDto(savedCompilation);
    }

    @Override
    @Transactional
    public CompilationDto updateCompilation(Long id, UpdateCompilationRequestDto dto) {
        log.info("Updating compilation id={} with data: {}", id, dto);

        if (id == null || dto == null) {
            log.error("Compilation id or UpdateCompilationRequestDto is null: id={}, dto={}", id, dto);
            throw new IllegalArgumentException("Compilation id and UpdateCompilationRequestDto cannot be null");
        }

        Compilation compilation = findCompilationById(id);

        if (dto.getPinned() != null) {
            compilation.setPinned(dto.getPinned());
            log.debug("Updated pinned status to {}", dto.getPinned());
        }

        if (dto.getTitle() != null && !dto.getTitle().isBlank()) {
            compilation.setTitle(dto.getTitle());
            log.debug("Updated title to {}", dto.getTitle());
        }

        if (dto.getEvents() != null) {
            List<Event> events = getEventsByIds(dto.getEvents());
            compilation.setEvents(events);
            log.debug("Updated events list with {} events", events.size());
        }

        Compilation updatedCompilation = compilationRepository.save(compilation);
        log.info("Compilation id={} successfully updated", id);

        return mapToCompilationDto(updatedCompilation);
    }

    private Compilation findCompilationById(Long id) {
        return compilationRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Compilation with id={} not found", id);
                    return new NotFoundException("Compilation with id=" + id + " not found");
                });
    }

    private List<Event> getEventsByIds(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            log.debug("No event IDs provided, returning empty list");
            return Collections.emptyList();
        }

        log.debug("Fetching events with IDs: {}", eventIds);
        List<Event> events = eventRepository.findAllById(eventIds);

        if (events.size() != eventIds.size()) {
            log.error("Not all events found for IDs: {}", eventIds);
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