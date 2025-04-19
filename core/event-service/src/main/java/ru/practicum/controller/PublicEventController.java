package ru.practicum.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.HitDto;
import ru.practicum.constant.Constants;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.enums.EventState;
import ru.practicum.exception.IncorrectValueException;
import ru.practicum.exception.NotFoundException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class PublicEventController {

    private final ru.practicum.service.EventService eventService;
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(Constants.JSON_TIME_FORMAT);


    @GetMapping
    public List<EventShortDto> getAll(
            @RequestParam(required = false) String text,
            @RequestParam(required = false) List<Long> categories,
            @RequestParam(required = false) Boolean paid,
            @RequestParam(required = false) @DateTimeFormat(pattern = Constants.JSON_TIME_FORMAT) LocalDateTime rangeStart,
            @RequestParam(required = false) @DateTimeFormat(pattern = Constants.JSON_TIME_FORMAT) LocalDateTime rangeEnd,
            @RequestParam(required = false, defaultValue = "false") Boolean onlyAvailable,
            @RequestParam(required = false, defaultValue = "0") Integer from,
            @RequestParam(required = false, defaultValue = "10") Integer size,
            HttpServletRequest httpRequest) {
        log.info("==> GET /events Public searching events with params: " +
                        "text {}, categories: {}, paid {}, rangeStart: {}, rangeEnd: {}, available {}, from: {}, size: {}",
                text, categories, paid, rangeStart, rangeEnd, onlyAvailable, from, size);

        if (rangeStart != null && rangeEnd != null && rangeStart.isAfter(rangeEnd)) {
            throw new IncorrectValueException("rangeStart of event can't be after rangeEnd");
        }

        EventSearchParams eventSearchParams = new EventSearchParams();
        PublicSearchParams publicSearchParams = new PublicSearchParams();
        publicSearchParams.setText(text);
        publicSearchParams.setCategories(categories);
        publicSearchParams.setPaid(paid);

        publicSearchParams.setRangeStart(rangeStart);
        publicSearchParams.setRangeEnd(rangeEnd);

        eventSearchParams.setPublicSearchParams(publicSearchParams);
        eventSearchParams.setFrom(from);
        eventSearchParams.setSize(size);

        HitDto hitDto = new HitDto(
                null,
                "ewm-service",
                httpRequest.getRequestURI(),
                httpRequest.getRemoteAddr(),
                LocalDateTime.now().format(dateTimeFormatter));

        List<EventShortDto> eventShortDtoList = eventService.getAllByPublic(eventSearchParams, hitDto);
        log.info("<== GET /events Returning public searching events. List size: {}",
                eventShortDtoList.size());
        return eventShortDtoList;
    }

    @GetMapping("/top")
    public List<EventShortDto> getTop(
            @RequestParam(required = false, defaultValue = "10") Integer count,
            HttpServletRequest httpRequest) {
        log.info("==> GET /events/top");

        HitDto hitDto = new HitDto(
                null,
                "ewm-service",
                httpRequest.getRequestURI(),
                httpRequest.getRemoteAddr(),
                LocalDateTime.now().format(dateTimeFormatter));

        List<EventShortDto> eventShortDtoList = eventService.getTopEvent(count, hitDto);
        log.info("<== GET /events Returning top {} events.", count);
        return eventShortDtoList;
    }

    @GetMapping("/top-view")
    public List<EventShortDto> getTopView(
            @RequestParam(required = false, defaultValue = "10") Integer count,
            HttpServletRequest httpRequest) {
        log.info("==> GET /events/top-view");

        HitDto hitDto = new HitDto(
                null,
                "ewm-service",
                httpRequest.getRequestURI(),
                httpRequest.getRemoteAddr(),
                LocalDateTime.now().format(dateTimeFormatter));

        List<EventShortDto> eventShortDtoList = eventService.getTopViewEvent(count, hitDto);
        log.info("<== GET /events Returning top view {} events.", count);
        return eventShortDtoList;
    }

    @GetMapping("/{id}")
    @Transactional
    public EventFullDto getById(
            @PathVariable Long id, HttpServletRequest httpRequest
    ) {
        log.info("==> GET /events/{}  Public getById", id);
        HitDto hitDto = new HitDto(
                null,
                "ewm-service",
                httpRequest.getRequestURI(),
                httpRequest.getRemoteAddr(),
                LocalDateTime.now().format(dateTimeFormatter));
        EventFullDto eventFullDto = eventService.getById(new EventGetByIdParams(null, id), hitDto);
        if (eventFullDto.state() != EventState.PUBLISHED) {
            throw new NotFoundException("Нет опубликованных событий с id " + id);
        }
        log.info("<== GET /events/{}  Public getById", id);
        return eventFullDto;
    }
}
