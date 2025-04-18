package ru.practicum.service;

import com.querydsl.core.types.dsl.BooleanExpression;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.HitDto;
import ru.practicum.HitStatDto;
import ru.practicum.client.LikeServiceClient;
import ru.practicum.client.LocationServiceClient;
import ru.practicum.client.RequestServiceClient;
import ru.practicum.client.StatClient;
import ru.practicum.client.UserServiceClient;
import ru.practicum.constant.Constants;
import ru.practicum.controller.EventGetByIdParams;
import ru.practicum.controller.EventSearchParams;
import ru.practicum.controller.EventUpdateParams;
import ru.practicum.controller.PublicSearchParams;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.dto.event.NewEventDto;
import ru.practicum.dto.location.LocationDto;
import ru.practicum.dto.user.UserDto;
import ru.practicum.enums.EventState;
import ru.practicum.enums.RequestStatus;
import ru.practicum.exception.AccessException;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.IncorrectValueException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.EventMapper;
import ru.practicum.model.Category;
import ru.practicum.model.Event;
import ru.practicum.repository.CategoryRepository;
import ru.practicum.repository.EventRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static ru.practicum.model.QEvent.event;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final UserServiceClient userServiceClient;
    private final EventMapper eventMapper;
    private final LocationServiceClient locationServiceClient;
    private final LikeServiceClient likeServiceClient;
    private final CategoryRepository categoryRepository;
    private final RequestServiceClient requestServiceClient;
    private final StatClient statClient;

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(Constants.JSON_TIME_FORMAT);

    /**
     * Creates a new event.
     *
     * @param userId      the ID of the user creating the event
     * @param newEventDto the DTO containing event details
     * @return EventFullDto of the created event
     * @throws NotFoundException if the category or user is not found
     */
    @Override
    @Transactional
    public EventFullDto create(long userId, NewEventDto newEventDto) {
        log.info("Creating event for userId={} with title={}", userId, newEventDto.title());

        UserDto user = userServiceClient.getById(userId);
        log.debug("Retrieved user: id={}", userId);

        Category category = findCategoryById(newEventDto.category());
        log.debug("Retrieved category: id={}", category.getId());

        LocationDto locationDto = locationServiceClient.create(userId, newEventDto.location());
        log.debug("Created location: id={}", locationDto.id());

        Event event = eventMapper.newEventDtoToEvent(newEventDto, userId, category, locationDto.id(), LocalDateTime.now());
        log.debug("Mapped event: title={}", event.getTitle());

        Event savedEvent = eventRepository.save(event);
        log.info("Event created successfully: id={}", savedEvent.getId());

        savedEvent.setInitiator(user);
        savedEvent.setLocation(locationDto);
        return eventMapper.eventToEventFullDto(savedEvent);
    }

    /**
     * Retrieves events created by a specific initiator.
     *
     * @param searchParams search parameters including initiator ID and pagination
     * @return list of EventShortDto objects
     */
    @Override
    @Transactional(readOnly = true)
    public List<EventShortDto> getAllByInitiator(EventSearchParams searchParams) {
        long initiatorId = searchParams.getPrivateSearchParams().getInitiatorId();
        log.info("Fetching events for initiatorId={}", initiatorId);

        UserDto user = userServiceClient.getById(initiatorId);
        log.debug("Retrieved user: id={}", initiatorId);

        Pageable page = PageRequest.of(searchParams.getFrom(), searchParams.getSize());
        log.debug("Created pageable: page={}, size={}", page.getPageNumber(), page.getPageSize());

        List<Event> events = eventRepository.findAllByInitiatorId(initiatorId, page);
        log.debug("Retrieved {} events", events.size());

        enrichEventsWithDetails(events);
        return events.stream()
                .map(eventMapper::eventToEventShortDto)
                .toList();
    }

    /**
     * Retrieves events based on public search parameters.
     *
     * @param searchParams search parameters including text, categories, etc.
     * @param hitDto       hit data for statistics
     * @return list of EventShortDto objects
     */
    @Override
    @Transactional(readOnly = true)
    public List<EventShortDto> getAllByPublic(EventSearchParams searchParams, HitDto hitDto) {
        log.info("Fetching public events with searchParams={}", searchParams);

        Pageable page = PageRequest.of(searchParams.getFrom(), searchParams.getSize());
        log.debug("Created pageable: page={}, size={}", page.getPageNumber(), page.getPageSize());

        BooleanExpression query = buildPublicSearchQuery(searchParams.getPublicSearchParams());
        List<Event> events = eventRepository.findAll(query, page).stream().toList();
        log.debug("Retrieved {} events", events.size());

        statClient.saveHit(hitDto);
        log.debug("Saved hit for statistics");

        enrichEventsWithDetails(events, searchParams.getPublicSearchParams().getRangeStart(),
                searchParams.getPublicSearchParams().getRangeEnd());
        return events.stream()
                .map(eventMapper::eventToEventShortDto)
                .toList();
    }

    /**
     * Retrieves top events by likes.
     *
     * @param count  number of events to return
     * @param hitDto hit data for statistics
     * @return list of EventShortDto objects
     */
    @Override
    @Transactional(readOnly = true)
    public List<EventShortDto> getTopEvent(Integer count, HitDto hitDto) {
        log.info("Fetching top {} events by likes", count);

        String rangeStart = LocalDateTime.now().minusYears(100).format(dateTimeFormatter);
        String rangeEnd = LocalDateTime.now().format(dateTimeFormatter);
        log.debug("Time range: start={}, end={}", rangeStart, rangeEnd);

        Map<Long, Long> likesMap = likeServiceClient.getTopLikedEventsIds(count);
        List<Long> eventIds = new ArrayList<>(likesMap.keySet());
        log.debug("Retrieved {} top event IDs", eventIds.size());

        List<Event> events = eventRepository.findAllByIdIn(eventIds);
        log.debug("Retrieved {} events", events.size());

        statClient.saveHit(hitDto);
        log.debug("Saved hit for statistics");

        enrichEventsWithDetails(events, LocalDateTime.parse(rangeStart, dateTimeFormatter),
                LocalDateTime.parse(rangeEnd, dateTimeFormatter));
        return events.stream()
                .map(eventMapper::eventToEventShortDto)
                .toList();
    }

    /**
     * Retrieves top events by views.
     *
     * @param count  number of events to return
     * @param hitDto hit data for statistics
     * @return list of EventShortDto objects
     */
    @Override
    @Transactional(readOnly = true)
    public List<EventShortDto> getTopViewEvent(Integer count, HitDto hitDto) {
        log.info("Fetching top {} events by views", count);

        String rangeStart = LocalDateTime.now().minusYears(100).format(dateTimeFormatter);
        String rangeEnd = LocalDateTime.now().format(dateTimeFormatter);
        log.debug("Time range: start={}, end={}", rangeStart, rangeEnd);

        statClient.saveHit(hitDto);
        log.debug("Saved hit for statistics");

        List<HitStatDto> stats = statClient.getStats(rangeStart, rangeEnd, null, true);
        log.debug("Retrieved {} stats entries", stats.size());

        Map<Long, Long> viewsMap = stats.stream()
                .filter(it -> it.getUri().matches("\\/events\\/\\d+$"))
                .collect(Collectors.groupingBy(
                        dto -> Long.parseLong(dto.getUri().replace("/events/", "")),
                        Collectors.summingLong(HitStatDto::getHits)));

        List<Event> events = eventRepository.findAllById(viewsMap.keySet()).stream()
                .peek(event -> event.setViews(viewsMap.getOrDefault(event.getId(), 0L)))
                .sorted((e1, e2) -> Long.compare(e2.getViews(), e1.getViews()))
                .limit(count)
                .collect(Collectors.toList());
        log.debug("Retrieved {} events after sorting", events.size());

        enrichEventsWithDetails(events);
        return events.stream()
                .map(eventMapper::eventToEventShortDto)
                .toList();
    }

    /**
     * Retrieves events based on admin search parameters.
     *
     * @param searchParams search parameters including users, categories, etc.
     * @return list of EventFullDto objects
     */
    @Override
    @Transactional(readOnly = true)
    public List<EventFullDto> getAllByAdmin(EventSearchParams searchParams) {
        log.info("Fetching events for admin with searchParams={}", searchParams);

        Pageable page = PageRequest.of(searchParams.getFrom(), searchParams.getSize());
        log.debug("Created pageable: page={}, size={}", page.getPageNumber(), page.getPageSize());

        BooleanExpression query = buildAdminSearchQuery(searchParams);
        List<Event> events = eventRepository.findAll(query, page).stream().toList();
        log.debug("Retrieved {} events", events.size());

        enrichEventsWithDetails(events);
        return events.stream()
                .map(eventMapper::eventToEventFullDto)
                .toList();
    }

    /**
     * Retrieves an event by ID, optionally checking initiator.
     *
     * @param params parameters including event ID and optional initiator ID
     * @param hitDto hit data for statistics
     * @return EventFullDto of the event
     * @throws NotFoundException if the event is not found
     */
    @Override
    @Transactional(readOnly = true)
    public EventFullDto getById(EventGetByIdParams params, HitDto hitDto) {
        log.info("Fetching event with id={} for initiatorId={}", params.eventId(), params.initiatorId());

        Event event;
        if (params.initiatorId() != null) {
            userServiceClient.checkExistence(params.initiatorId());
            event = eventRepository.findByInitiatorIdAndId(params.initiatorId(), params.eventId())
                    .orElseThrow(() -> {
                        log.error("Event with id={} not found for initiatorId={}", params.eventId(), params.initiatorId());
                        return new NotFoundException(String.format(
                                "Event with id=%d created by user with id=%d not found", params.eventId(), params.initiatorId()));
                    });
        } else {
            event = findEventById(params.eventId());
            statClient.saveHit(hitDto);
            log.debug("Saved hit for statistics");

            List<HitStatDto> stats = statClient.getStats("", "", List.of("/events/" + params.eventId()), true);
            long views = stats.stream().mapToLong(HitStatDto::getHits).sum();
            event.setViews(views);
            log.debug("Set views={} for eventId={}", views, event.getId());

            event.setConfirmedRequests(requestServiceClient.countByStatusAndEventId(RequestStatus.CONFIRMED, event.getId()));
            event.setLikes(likeServiceClient.getCountByEventId(event.getId()));
            event.setLocation(locationServiceClient.getById(event.getLocationId()));
        }

        UserDto initiator = userServiceClient.getById(event.getInitiatorId());
        event.setInitiator(initiator);
        log.info("Retrieved event: id={}, title={}", event.getId(), event.getTitle());

        return eventMapper.eventToEventFullDto(event);
    }

    /**
     * Updates an existing event.
     *
     * @param eventId      the ID of the event to update
     * @param updateParams update parameters for user or admin
     * @return EventFullDto of the updated event
     * @throws NotFoundException if the event or category is not found
     */
    @Override
    @Transactional
    public EventFullDto update(long eventId, EventUpdateParams updateParams) {
        log.info("Updating event with id={} for userId={}", eventId, updateParams.userId());

        Event event = findEventById(eventId);
        log.debug("Retrieved event: id={}, state={}", event.getId(), event.getState());

        if (updateParams.updateEventUserRequest() != null) {
            updateEventForUser(event, updateParams);
        } else if (updateParams.updateEventAdminRequest() != null) {
            updateEventForAdmin(event, updateParams);
        }

        Event updatedEvent = eventRepository.save(event);
        log.info("Event updated successfully: id={}", updatedEvent.getId());

        enrichEventWithDetails(updatedEvent);
        return eventMapper.eventToEventFullDto(updatedEvent);
    }

    /**
     * Retrieves an event by ID for internal use.
     *
     * @param eventId the ID of the event
     * @return EventFullDto of the event
     * @throws NotFoundException if the event is not found
     */
    @Override
    @Transactional(readOnly = true)
    public EventFullDto getByIdInternal(long eventId) {
        log.info("Fetching event internally with id={}", eventId);

        Event event = findEventById(eventId);
        log.debug("Retrieved event: id={}, title={}", event.getId(), event.getTitle());

        UserDto initiator = userServiceClient.getById(event.getInitiatorId());
        event.setInitiator(initiator);
        return eventMapper.eventToEventFullDto(event);
    }

    /**
     * Helper method to find an event by ID.
     */
    private Event findEventById(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> {
                    log.error("Event with id={} not found", eventId);
                    return new NotFoundException(String.format("Event with id=%d not found", eventId));
                });
    }

    /**
     * Helper method to find a category by ID.
     */
    private Category findCategoryById(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> {
                    log.error("Category with id={} not found", categoryId);
                    return new NotFoundException(String.format("Category with id=%d not found", categoryId));
                });
    }

    /**
     * Enriches events with additional details (likes, locations, users, etc.).
     */
    private void enrichEventsWithDetails(List<Event> events) {
        enrichEventsWithDetails(events, null, null);
    }

    private void enrichEventsWithDetails(List<Event> events, LocalDateTime rangeStart, LocalDateTime rangeEnd) {
        if (events.isEmpty()) {
            log.debug("No events to enrich");
            return;
        }

        List<Long> eventIds = events.stream().map(Event::getId).toList();
        Map<Long, Long> likesMap = likeServiceClient.getAllEventsLikesByIds(eventIds);
        Map<Long, Long> confirmedRequestsMap = requestServiceClient.countByStatusAndEventsIds(RequestStatus.CONFIRMED, eventIds);
        Map<Long, LocationDto> locationMap = locationServiceClient.getAllById(events.stream().map(Event::getLocationId).toList());
        Map<Long, UserDto> userMap = userServiceClient.getAll(events.stream().map(Event::getInitiatorId).toList());

        for (Event event : events) {
            if (rangeStart != null && rangeEnd != null) {
                List<HitStatDto> stats = statClient.getStats(
                        rangeStart.format(dateTimeFormatter),
                        rangeEnd.format(dateTimeFormatter),
                        List.of("/event/" + event.getId()),
                        false);
                long views = stats.stream().mapToLong(HitStatDto::getHits).sum();
                event.setViews(views);
            }
            event.setLikes(likesMap.getOrDefault(event.getId(), 0L));
            event.setConfirmedRequests(confirmedRequestsMap.getOrDefault(event.getId(), 0L));
            event.setLocation(locationMap.get(event.getLocationId()));
            event.setInitiator(userMap.get(event.getInitiatorId()));
        }
        log.debug("Enriched {} events with details", events.size());
    }

    /**
     * Enriches a single event with additional details.
     */
    private void enrichEventWithDetails(Event event) {
        event.setLikes(likeServiceClient.getCountByEventId(event.getId()));
        event.setConfirmedRequests(requestServiceClient.countByStatusAndEventId(RequestStatus.CONFIRMED, event.getId()));
        event.setLocation(locationServiceClient.getById(event.getLocationId()));
        event.setInitiator(userServiceClient.getById(event.getInitiatorId()));
        log.debug("Enriched event with id={}", event.getId());
    }

    /**
     * Builds a QueryDSL expression for public event search.
     */
    private BooleanExpression buildPublicSearchQuery(PublicSearchParams params) {
        BooleanExpression query = event.isNotNull();

        if (params.getText() != null) {
            query = query.andAnyOf(
                    event.annotation.likeIgnoreCase(params.getText()),
                    event.description.likeIgnoreCase(params.getText()));
            log.debug("Added text filter: {}", params.getText());
        }
        if (params.getCategories() != null) {
            query = query.and(event.category.id.in(params.getCategories()));
            log.debug("Added categories filter: {}", params.getCategories());
        }
        if (params.getPaid() != null) {
            query = query.and(event.paid.eq(params.getPaid()));
            log.debug("Added paid filter: {}", params.getPaid());
        }

        LocalDateTime rangeStart = params.getRangeStart();
        LocalDateTime rangeEnd = params.getRangeEnd();
        if (rangeStart != null && rangeEnd != null) {
            query = query.and(event.eventDate.between(rangeStart, rangeEnd));
            log.debug("Added date range filter: start={}, end={}", rangeStart, rangeEnd);
        } else if (rangeStart != null) {
            query = query.and(event.eventDate.after(rangeStart));
            log.debug("Added start date filter: {}", rangeStart);
        } else if (rangeEnd != null) {
            query = query.and(event.eventDate.before(rangeEnd));
            log.debug("Added end date filter: {}", rangeEnd);
        } else {
            query = query.and(event.eventDate.after(LocalDateTime.now()));
            log.debug("Added default future date filter");
        }

        return query;
    }

    /**
     * Builds a QueryDSL expression for admin event search.
     */
    private BooleanExpression buildAdminSearchQuery(EventSearchParams params) {
        BooleanExpression query = event.isNotNull();

        if (params.getAdminSearchParams().getUsers() != null) {
            query = query.and(event.initiatorId.in(params.getAdminSearchParams().getUsers()));
            log.debug("Added users filter: {}", params.getAdminSearchParams().getUsers());
        }
        if (params.getAdminSearchParams().getCategories() != null) {
            query = query.and(event.category.id.in(params.getAdminSearchParams().getCategories()));
            log.debug("Added categories filter: {}", params.getAdminSearchParams().getCategories());
        }
        if (params.getAdminSearchParams().getStates() != null) {
            query = query.and(event.state.in(params.getAdminSearchParams().getStates()));
            log.debug("Added states filter: {}", params.getAdminSearchParams().getStates());
        }

        LocalDateTime rangeStart = params.getAdminSearchParams().getRangeStart();
        LocalDateTime rangeEnd = params.getAdminSearchParams().getRangeEnd();
        if (rangeStart != null && rangeEnd != null) {
            query = query.and(event.eventDate.between(rangeStart, rangeEnd));
            log.debug("Added date range filter: start={}, end={}", rangeStart, rangeEnd);
        } else if (rangeStart != null) {
            query = query.and(event.eventDate.after(rangeStart));
            log.debug("Added start date filter: {}", rangeStart);
        } else if (rangeEnd != null) {
            query = query.and(event.eventDate.before(rangeEnd));
            log.debug("Added end date filter: {}", rangeEnd);
        }

        return query;
    }

    /**
     * Updates an event for a user.
     */
    private void updateEventForUser(Event event, EventUpdateParams params) {
        userServiceClient.checkExistence(params.userId());
        if (!params.userId().equals(event.getInitiatorId())) {
            log.error("UserId={} is not the initiator of eventId={}", params.userId(), event.getId());
            throw new AccessException(String.format("User with id=%d does not initiate this event", params.userId()));
        }

        if (event.getState() != EventState.PENDING && event.getState() != EventState.CANCELED) {
            log.error("Cannot update eventId={} with state={}", event.getId(), event.getState());
            throw new ConflictException("Only PENDING or CANCELED events can be changed");
        }

        var request = params.updateEventUserRequest();
        if (request.category() != null) {
            Category category = findCategoryById(request.category());
            event.setCategory(category);
            log.debug("Updated category to id={}", category.getId());
        }
        if (request.eventDate() != null && request.eventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            log.error("Event date for eventId={} is too soon: {}", event.getId(), request.eventDate());
            throw new ConflictException("Event date must be at least 2 hours from now");
        }
        if (request.stateAction() != null) {
            switch (request.stateAction()) {
                case CANCEL_REVIEW -> event.setState(EventState.CANCELED);
                case SEND_TO_REVIEW -> event.setState(EventState.PENDING);
            }
            log.debug("Updated state to: {}", event.getState());
        }
        if (request.location() != null) {
            event.setLocationId(request.location().id());
            log.debug("Updated location to id={}", request.location().id());
        }

        eventMapper.updateEventUserRequestToEvent(event, request);
        log.debug("Applied user updates to eventId={}", event.getId());
    }

    /**
     * Updates an event for an admin.
     */
    private void updateEventForAdmin(Event event, EventUpdateParams params) {
        var request = params.updateEventAdminRequest();
        if (request.category() != null) {
            Category category = findCategoryById(request.category());
            event.setCategory(category);
            log.debug("Updated category to id={}", category.getId());
        }
        if (event.getState() != EventState.PENDING) {
            log.error("Cannot update eventId={} with state={}", event.getId(), event.getState());
            throw new ConflictException("Only PENDING events can be changed by admin");
        }
        if (request.eventDate() != null && request.eventDate().isBefore(LocalDateTime.now().plusHours(1))) {
            log.error("Event date for eventId={} is too soon: {}", event.getId(), request.eventDate());
            throw new IncorrectValueException("Event date must be at least 1 hour from now");
        }

        eventMapper.updateEventAdminRequestToEvent(event, request);
        log.debug("Applied admin updates to eventId={}", event.getId());
    }
}