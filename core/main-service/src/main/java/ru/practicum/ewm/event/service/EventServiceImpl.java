package ru.practicum.ewm.event.service;

import com.querydsl.core.types.dsl.BooleanExpression;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.HitDto;
import ru.practicum.HitStatDto;
import ru.practicum.client.StatClient;
import ru.practicum.ewm.category.model.Category;
import ru.practicum.ewm.category.repository.CategoryRepository;
import ru.practicum.ewm.config.Constants;
import ru.practicum.ewm.event.controller.*;
import ru.practicum.ewm.event.dto.*;
import ru.practicum.ewm.event.model.*;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.exception.*;
import ru.practicum.ewm.location.model.Location;
import ru.practicum.ewm.location.repository.LocationRepository;
import ru.practicum.ewm.event.mapper.EventMapper;
import ru.practicum.ewm.request.model.RequestStatus;
import ru.practicum.ewm.request.repository.RequestRepository;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static ru.practicum.ewm.event.model.QEvent.event;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(Constants.JSON_TIME_FORMAT);
    private static final int MIN_HOURS_BEFORE_EVENT = 2;

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final EventMapper eventMapper;
    private final LocationRepository locationRepository;
    private final CategoryRepository categoryRepository;
    private final RequestRepository requestRepository;
    private final StatClient statClient;

    @Override
    @Transactional
    public EventFullDto create(long userId, NewEventDto newEventDto) {
        User initiator = getUserById(userId);
        Category category = getCategoryById(newEventDto.category());
        Location location = locationRepository.save(newEventDto.location());

        Event event = eventMapper.newEventDtoToEvent(
                newEventDto,
                initiator,
                category,
                location,
                LocalDateTime.now()
        );

        Event savedEvent = eventRepository.save(event);
        return eventMapper.eventToEventFullDto(savedEvent);
    }

    @Override
    public List<EventShortDto> getAllByInitiator(EventSearchParams searchParams) {
        long initiatorId = searchParams.getPrivateSearchParams().getInitiatorId();
        getUserById(initiatorId); // Проверка существования пользователя

        Pageable page = PageRequest.of(searchParams.getFrom(), searchParams.getSize());
        List<Event> receivedEvents = eventRepository.findAllByInitiatorId(initiatorId, page);

        enrichEventsWithAdditionalData(receivedEvents);

        return receivedEvents.stream()
                .map(eventMapper::eventToEventShortDto)
                .toList();
    }

    @Override
    public List<EventShortDto> getAllByPublic(EventSearchParams searchParams, HitDto hitDto) {
        Pageable page = PageRequest.of(searchParams.getFrom(), searchParams.getSize());
        PublicSearchParams publicSearchParams = searchParams.getPublicSearchParams();

        BooleanExpression booleanExpression = buildPublicSearchExpression(publicSearchParams);
        LocalDateTime[] dateRange = determineDateRange(publicSearchParams);

        List<Event> eventListBySearch = eventRepository.findAll(booleanExpression, page).stream().toList();
        statClient.saveHit(hitDto);

        enrichEventsWithStatsAndRequests(eventListBySearch, dateRange[0], dateRange[1]);

        return eventListBySearch.stream()
                .map(eventMapper::eventToEventShortDto)
                .toList();
    }

    @Override
    public List<EventShortDto> getTopEvent(Integer count, HitDto hitDto) {
        String rangeEnd = LocalDateTime.now().format(DATE_TIME_FORMATTER);
        String rangeStart = LocalDateTime.now().minusYears(100).format(DATE_TIME_FORMATTER);

        List<Event> eventListBySearch = eventRepository.findTop(count);
        statClient.saveHit(hitDto);

        enrichEventsWithStatsAndRequests(eventListBySearch, rangeStart, rangeEnd);

        return eventListBySearch.stream()
                .map(eventMapper::eventToEventShortDto)
                .toList();
    }

    @Override
    public List<EventShortDto> getTopViewEvent(Integer count, HitDto hitDto) {
        String rangeEnd = LocalDateTime.now().format(DATE_TIME_FORMATTER);
        String rangeStart = LocalDateTime.now().minusYears(100).format(DATE_TIME_FORMATTER);

        statClient.saveHit(hitDto);

        List<HitStatDto> hitStatDtoList = statClient.getStats(
                rangeStart,
                rangeEnd,
                null,
                true);

        Map<Long, Long> eventsViews = hitStatDtoList.stream()
                .filter(it -> it.getUri().matches("\\/events\\/\\d+$"))
                .collect(Collectors.groupingBy(
                        dto -> Long.parseLong(dto.getUri().replace("/events/", "")),
                        Collectors.summingLong(HitStatDto::getHits)
                ));

        List<Event> topEvents = getTopEventsByViews(eventsViews, count);
        return topEvents.stream()
                .map(eventMapper::eventToEventShortDto)
                .toList();
    }

    @Override
    public List<EventFullDto> getAllByAdmin(EventSearchParams searchParams) {
        Pageable page = PageRequest.of(searchParams.getFrom(), searchParams.getSize());
        BooleanExpression booleanExpression = buildAdminSearchExpression(searchParams);

        List<Event> receivedEventList = eventRepository.findAll(booleanExpression, page).stream().toList();
        enrichEventsWithAdditionalData(receivedEventList);

        return receivedEventList.stream()
                .map(eventMapper::eventToEventFullDto)
                .toList();
    }

    @Override
    public EventFullDto getById(EventGetByIdParams params, HitDto hitDto) {
        Event receivedEvent = findEventWithAccessCheck(params);

        if (params.initiatorId() == null) {
            statClient.saveHit(hitDto);
            enrichEventWithStatsAndRequests(receivedEvent, "", "");
        }

        return eventMapper.eventToEventFullDto(receivedEvent);
    }

    @Override
    @Transactional
    public EventFullDto update(long eventId, EventUpdateParams updateParams) {
        Event event = getEventById(eventId);
        Event updatedEvent;

        if (updateParams.updateEventUserRequest() != null) {
            updatedEvent = processUserUpdate(event, updateParams);
        } else if (updateParams.updateEventAdminRequest() != null) {
            updatedEvent = processAdminUpdate(event, updateParams);
        } else {
            updatedEvent = event;
        }

        updatedEvent.setLikes(eventRepository.countLikesByEventId(updatedEvent.getId()));
        return eventMapper.eventToEventFullDto(updatedEvent);
    }

    @Override
    @Transactional
    public EventShortDto addLike(long userId, long eventId) {
        User user = getUserById(userId);
        Event event = getEventById(eventId);

        validateEventIsPublished(event);
        eventRepository.addLike(userId, eventId);

        event.setLikes(eventRepository.countLikesByEventId(eventId));
        return eventMapper.eventToEventShortDto(event);
    }

    @Override
    @Transactional
    public void deleteLike(long userId, long eventId) {
        User user = getUserById(userId);
        Event event = getEventById(eventId);

        if (!eventRepository.checkLikeExisting(userId, eventId)) {
            throw new NotFoundException("Like for event: " + eventId + " by user: " + user.getId() + " not exist");
        }

        eventRepository.deleteLike(userId, eventId);
    }

    private User getUserById(long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id " + userId + " not found"));
    }

    private Category getCategoryById(long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Category with id " + categoryId + " not found"));
    }

    private Event getEventById(long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id " + eventId + " not found"));
    }

    private BooleanExpression buildPublicSearchExpression(PublicSearchParams params) {
        BooleanExpression expression = event.isNotNull();

        if (params.getText() != null) {
            expression = expression.andAnyOf(
                    event.annotation.likeIgnoreCase(params.getText()),
                    event.description.likeIgnoreCase(params.getText())
            );
        }

        if (params.getCategories() != null) {
            expression = expression.and(event.category.id.in(params.getCategories()));
        }

        if (params.getPaid() != null) {
            expression = expression.and(event.paid.eq(params.getPaid()));
        }

        return expression;
    }

    private LocalDateTime[] determineDateRange(PublicSearchParams params) {
        LocalDateTime rangeStart = params.getRangeStart();
        LocalDateTime rangeEnd = params.getRangeEnd();

        if (rangeStart != null && rangeEnd != null) {
            return new LocalDateTime[]{rangeStart, rangeEnd};
        } else if (rangeStart != null) {
            return new LocalDateTime[]{rangeStart, rangeStart.plusYears(100)};
        } else if (rangeEnd != null) {
            return new LocalDateTime[]{LocalDateTime.now(), rangeEnd};
        } else {
            return new LocalDateTime[]{LocalDateTime.now(), LocalDateTime.now().plusYears(100)};
        }
    }

    private BooleanExpression buildAdminSearchExpression(EventSearchParams searchParams) {
        BooleanExpression expression = event.isNotNull();
        AdminSearchParams adminParams = searchParams.getAdminSearchParams();

        if (adminParams.getUsers() != null) {
            expression = expression.and(event.initiator.id.in(adminParams.getUsers()));
        }

        if (adminParams.getCategories() != null) {
            expression = expression.and(event.category.id.in(adminParams.getCategories()));
        }

        if (adminParams.getStates() != null) {
            expression = expression.and(event.state.in(adminParams.getStates()));
        }

        LocalDateTime rangeStart = adminParams.getRangeStart();
        LocalDateTime rangeEnd = adminParams.getRangeEnd();

        if (rangeStart != null && rangeEnd != null) {
            expression = expression.and(event.eventDate.between(rangeStart, rangeEnd));
        } else if (rangeStart != null) {
            expression = expression.and(event.eventDate.after(rangeStart));
        } else if (rangeEnd != null) {
            expression = expression.and(event.eventDate.before(rangeEnd));
        }

        return expression;
    }

    private void enrichEventsWithAdditionalData(List<Event> events) {
        events.forEach(event -> {
            event.setConfirmedRequests(
                    requestRepository.countByStatusAndEventId(RequestStatus.CONFIRMED, event.getId()));
            event.setLikes(eventRepository.countLikesByEventId(event.getId()));
        });
    }

    private void enrichEventsWithStatsAndRequests(List<Event> events, LocalDateTime rangeStart, LocalDateTime rangeEnd) {
        enrichEventsWithStatsAndRequests(
                events,
                rangeStart.format(DATE_TIME_FORMATTER),
                rangeEnd.format(DATE_TIME_FORMATTER)
        );
    }

    private void enrichEventsWithStatsAndRequests(List<Event> events, String rangeStart, String rangeEnd) {
        events.forEach(event -> {
            enrichEventWithStatsAndRequests(event, rangeStart, rangeEnd);
        });
    }

    private void enrichEventWithStatsAndRequests(Event event, String rangeStart, String rangeEnd) {
        List<HitStatDto> hitStatDtoList = statClient.getStats(
                rangeStart,
                rangeEnd,
                List.of("/events/" + event.getId()),
                true);

        long views = hitStatDtoList.stream()
                .mapToLong(HitStatDto::getHits)
                .sum();

        event.setViews(views);
        event.setConfirmedRequests(
                requestRepository.countByStatusAndEventId(RequestStatus.CONFIRMED, event.getId()));
        event.setLikes(eventRepository.countLikesByEventId(event.getId()));
    }

    private List<Event> getTopEventsByViews(Map<Long, Long> eventsViews, int limit) {
        Set<Long> eventIds = eventsViews.keySet();
        List<Event> events = eventRepository.findAllById(eventIds);
        Map<Long, Event> eventMap = events.stream()
                .collect(Collectors.toMap(Event::getId, e -> e));

        return eventsViews.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(limit)
                .map(entry -> {
                    Event event = eventMap.get(entry.getKey());
                    if (event != null) {
                        event.setViews(entry.getValue());
                    }
                    return event;
                })
                .filter(Objects::nonNull)
                .toList();
    }

    private Event findEventWithAccessCheck(EventGetByIdParams params) {
        if (params.initiatorId() != null) {
            getUserById(params.initiatorId());
            return eventRepository.findByInitiatorIdAndId(params.initiatorId(), params.eventId())
                    .orElseThrow(() -> new NotFoundException(
                            "Event with id " + params.eventId() +
                                    " created by user with id " + params.initiatorId() + " not found"));
        }
        return getEventById(params.eventId());
    }

    private Event processUserUpdate(Event event, EventUpdateParams updateParams) {
        validateUserIsInitiator(updateParams.userId(), event);
        validateEventStateForUserUpdate(event.getState());

        UpdateEventUserRequest updateRequest = updateParams.updateEventUserRequest();
        if (updateRequest.category() != null) {
            event.setCategory(getCategoryById(updateRequest.category()));
        }

        validateEventDate(updateRequest.eventDate());

        processStateAction(event, updateRequest.stateAction());
        eventMapper.updateEventUserRequestToEvent(event, updateRequest);

        return eventRepository.save(event);
    }

    private Event processAdminUpdate(Event event, EventUpdateParams updateParams) {
        validateEventStateForAdminUpdate(event.getState());

        UpdateEventAdminRequest updateRequest = updateParams.updateEventAdminRequest();
        if (updateRequest.category() != null) {
            event.setCategory(getCategoryById(updateRequest.category()));
        }

        validateEventDate(updateRequest.eventDate());
        eventMapper.updateEventAdminRequestToEvent(event, updateRequest);

        return eventRepository.save(event);
    }

    private void validateUserIsInitiator(long userId, Event event) {
        if (userId != event.getInitiator().getId()) {
            throw new AccessException("User with id = " + userId + " do not initiate this event");
        }
    }

    private void validateEventStateForUserUpdate(EventState state) {
        if (state != EventState.PENDING && state != EventState.CANCELED) {
            throw new ConflictException("User. Cannot update event: only pending or canceled events can be changed");
        }
    }

    private void validateEventStateForAdminUpdate(EventState state) {
        if (state != EventState.PENDING) {
            throw new ConflictException("Admin. Cannot update event: only pending events can be changed");
        }
    }

    private void validateEventDate(LocalDateTime eventDate) {
        if (eventDate != null && eventDate.isBefore(LocalDateTime.now().plusHours(MIN_HOURS_BEFORE_EVENT))) {
            throw new ConflictException("Cannot update event: event date must be not earlier then after " +
                    MIN_HOURS_BEFORE_EVENT + " hours");
        }
    }

    private void validateEventIsPublished(Event event) {
        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Event with id " + event.getId() + " is not published");
        }
    }

    private void processStateAction(Event event, StateAction stateAction) {
        if (stateAction != null) {
            switch (stateAction) {
                case CANCEL_REVIEW -> event.setState(EventState.CANCELED);
                case SEND_TO_REVIEW -> event.setState(EventState.PENDING);
            }
        }
    }
}