package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
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

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompilationServiceImpl implements CompilationService {

    private final CompilationRepository compilationRepository;
    private final CompilationMapper compilationMapper;
    private final EventRepository eventRepository;
    private final EventMapper eventMapper;


    @Override
    public List<CompilationDto> getAll(Boolean pinned, int from, int size) {
        log.info("Fetching compilations: pinned={}, from={}, size={}", pinned, from, size);

        Pageable pageable = PageRequest.of(from > 0 ? from / size : 0, size);
        log.debug("Created pageable: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());

        Page<Compilation> compilationsPage = pinned == null
                ? compilationRepository.findAll(pageable)
                : compilationRepository.findAllByPinned(pinned, pageable);
        log.debug("Retrieved {} compilations", compilationsPage.getContent().size());

        List<CompilationDto> compilationDtos = compilationsPage.getContent().stream()
                .map(compilation -> compilationMapper.toCompilationDto(
                        compilation,
                        compilation.getEvents().stream()
                                .map(eventMapper::eventToEventShortDto)
                                .toList()))
                .toList();
        log.info("Returning {} compilations", compilationDtos.size());

        return compilationDtos;
    }

    @Override
    public CompilationDto getById(Long id) {
        log.info("Fetching compilation with id={}", id);

        Compilation compilation = findCompilationById(id);
        log.debug("Retrieved compilation: id={}, title={}", compilation.getId(), compilation.getTitle());

        List<EventShortDto> events = compilation.getEvents().stream()
                .map(eventMapper::eventToEventShortDto)
                .toList();
        log.debug("Mapped {} events for compilation id={}", events.size(), id);

        CompilationDto compilationDto = compilationMapper.toCompilationDto(compilation, events);
        log.info("Returning compilation: id={}, title={}", compilationDto.getId(), compilationDto.getTitle());

        return compilationDto;
    }

    @Override
    public void delete(long compilationId) {
        log.info("Deleting compilation with id={}", compilationId);

        Compilation compilation = findCompilationById(compilationId);
        log.debug("Compilation found for deletion: id={}, title={}", compilation.getId(), compilation.getTitle());

        compilationRepository.deleteById(compilationId);
        log.info("Compilation deleted successfully: id={}", compilationId);
    }

    @Override
    public CompilationDto createCompilation(NewCompilationDto dto) {
        log.info("Creating new compilation with title={}", dto.getTitle());

        List<Event> events = new ArrayList<>();
        if (dto.getEvents() != null && !dto.getEvents().isEmpty()) {
            events = eventRepository.findAllById(dto.getEvents());
            log.debug("Retrieved {} events for compilation: eventIds={}", events.size(), dto.getEvents());
        } else {
            log.debug("No events provided for compilation");
        }

        Compilation compilation = compilationMapper.toCompilation(dto, events);
        log.debug("Mapped NewCompilationDto to Compilation: {}", compilation);

        Compilation savedCompilation = compilationRepository.save(compilation);
        log.info("Compilation created successfully: id={}, title={}", savedCompilation.getId(), savedCompilation.getTitle());

        List<EventShortDto> eventDtos = savedCompilation.getEvents().stream()
                .map(eventMapper::eventToEventShortDto)
                .toList();
        log.debug("Mapped {} events for compilation id={}", eventDtos.size(), savedCompilation.getId());

        return compilationMapper.toCompilationDto(savedCompilation, eventDtos);
    }

    @Override
    public CompilationDto updateCompilation(Long id, UpdateCompilationRequestDto dto) {
        log.info("Updating compilation with id={}", id);

        Compilation compilation = findCompilationById(id);
        log.debug("Retrieved compilation: id={}, title={}", compilation.getId(), compilation.getTitle());

        if (dto.getPinned() != null) {
            compilation.setPinned(dto.getPinned());
            log.debug("Updated pinned status to: {}", dto.getPinned());
        }
        if (dto.getTitle() != null) {
            compilation.setTitle(dto.getTitle());
            log.debug("Updated title to: {}", dto.getTitle());
        }
        if (dto.getEvents() != null) {
            List<Event> events = new ArrayList<>();
            if (!dto.getEvents().isEmpty()) {
                events = eventRepository.findAllById(dto.getEvents());
                log.debug("Retrieved {} events for compilation: eventIds={}", events.size(), dto.getEvents());
            } else {
                log.debug("Empty event list provided; clearing events");
            }
            compilation.setEvents(events);
        }

        Compilation updatedCompilation = compilationRepository.save(compilation);
        log.info("Compilation updated successfully: id={}, title={}", updatedCompilation.getId(), updatedCompilation.getTitle());

        List<EventShortDto> eventDtos = updatedCompilation.getEvents().stream()
                .map(eventMapper::eventToEventShortDto)
                .toList();
        log.debug("Mapped {} events for compilation id={}", eventDtos.size(), updatedCompilation.getId());

        return compilationMapper.toCompilationDto(updatedCompilation, eventDtos);
    }

    private Compilation findCompilationById(Long compilationId) {
        return compilationRepository.findById(compilationId)
                .orElseThrow(() -> {
                    log.error("Compilation with id={} not found", compilationId);
                    return new NotFoundException(String.format("Compilation with id=%d not found", compilationId));
                });
    }
}