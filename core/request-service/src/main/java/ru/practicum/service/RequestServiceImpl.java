package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.client.EventServiceClient;
import ru.practicum.client.UserServiceClient;
import ru.practicum.controller.PrivateUpdateRequestParams;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.request.EventRequestStatusUpdateResult;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.dto.user.UserDto;
import ru.practicum.enums.EventState;
import ru.practicum.enums.RequestStatus;
import ru.practicum.exception.AccessException;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.RequestMapper;
import ru.practicum.model.Request;
import ru.practicum.repository.RequestRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequestServiceImpl implements RequestService {

    private final RequestRepository requestRepository;
    private final UserServiceClient userServiceClient;
    private final EventServiceClient eventServiceClient;
    private final RequestMapper requestMapper;

    @Override
    @Transactional
    public ParticipationRequestDto create(long userId, long eventId) {
        log.info("Creating participation request for userId={} and eventId={}", userId, eventId);

        UserDto user = userServiceClient.getById(userId);
        EventFullDto event = eventServiceClient.getById(eventId);
        log.debug("Retrieved user: {}, event: {}", user, event);

        validateCreateRequest(user, event);

        long confirmedRequests = requestRepository.countByStatusAndEventId(RequestStatus.CONFIRMED, eventId);
        log.debug("Current confirmed requests for eventId={}: {}", eventId, confirmedRequests);

        Request request = requestMapper.toRequest(user, event);
        setInitialStatus(request, event);

        Request savedRequest = requestRepository.save(request);
        log.info("Participation request created with id={}", savedRequest.getId());

        return requestMapper.toParticipationRequestDto(savedRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParticipationRequestDto> getAllOwnRequests(long userId) {
        log.info("Fetching all requests for userId={}", userId);

        userServiceClient.checkExistence(userId);
        List<Request> requests = requestRepository.getAllByRequesterId(userId);

        log.info("Found {} requests for userId={}", requests.size(), userId);
        return requests.stream()
                .map(requestMapper::toParticipationRequestDto)
                .toList();
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancel(long userId, long requestId) {
        log.info("Cancelling requestId={} for userId={}", requestId, userId);

        userServiceClient.checkExistence(userId);
        Request request = findRequestById(requestId);

        validateRequestOwnership(userId, request);

        request.setStatus(RequestStatus.CANCELED);
        Request canceledRequest = requestRepository.save(request);
        log.info("Request id={} successfully canceled", requestId);

        return requestMapper.toParticipationRequestDto(canceledRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParticipationRequestDto> getAllForOwnEvent(long userId, long eventId) {
        log.info("Fetching all requests for eventId={} by userId={}", eventId, userId);

        UserDto user = userServiceClient.getById(userId);
        EventFullDto event = eventServiceClient.getById(eventId);
        log.debug("Retrieved user: {}, event: {}", user, event);

        validateEventOwnership(user, event);

        List<Request> requests = requestRepository.getAllByEventId(eventId);
        log.info("Found {} requests for eventId={}", requests.size(), eventId);

        return requests.stream()
                .map(requestMapper::toParticipationRequestDto)
                .toList();
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateStatus(PrivateUpdateRequestParams params) {
        log.info("Updating request statuses for eventId={} by userId={}", params.eventId(), params.userId());

        UserDto user = userServiceClient.getById(params.userId());
        EventFullDto event = eventServiceClient.getById(params.eventId());
        log.debug("Retrieved user: {}, event: {}", user, event);

        validateEventOwnership(user, event);

        List<Long> requestIds = params.eventRequestStatusUpdateRequest().requestIds();
        RequestStatus newStatus = params.eventRequestStatusUpdateRequest().status();
        List<Request> requests = requestRepository.findAllByIdInAndEventId(requestIds, params.eventId());

        validateRequestsForUpdate(requests, event);

        long confirmedCount = requestRepository.countByStatusAndEventId(RequestStatus.CONFIRMED, event.id());
        log.debug("Current confirmed requests: {}", confirmedCount);

        updateRequestsStatus(requests, newStatus, event, confirmedCount);

        List<ParticipationRequestDto> confirmedRequests = fetchConfirmedRequests(event);
        List<ParticipationRequestDto> rejectedRequests = fetchRejectedRequests(event);

        log.info("Status update completed: {} confirmed, {} rejected", confirmedRequests.size(), rejectedRequests.size());
        return new EventRequestStatusUpdateResult(confirmedRequests, rejectedRequests);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByStatusAndEventId(RequestStatus status, long eventId) {
        log.debug("Counting requests with status={} for eventId={}", status, eventId);
        long count = requestRepository.countByStatusAndEventId(status, eventId);
        log.debug("Found {} requests", count);
        return count;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Long> countByStatusAndEventsIds(RequestStatus status, List<Long> eventIds) {
        log.info("Counting requests with status={} for eventIds={}", status, eventIds);

        if (eventIds == null || eventIds.isEmpty()) {
            log.warn("Empty or null eventIds provided, returning empty map");
            return Map.of();
        }

        List<Map<String, Long>> results = requestRepository.countByStatusAndEventsIds(status.toString(), eventIds);
        Map<Long, Long> eventRequestsCount = results.stream()
                .collect(Collectors.toMap(
                        row -> row.get("EVENT_ID"),
                        row -> row.get("EVENT_COUNT")
                ));

        log.info("Found counts for {} events", eventRequestsCount.size());
        return eventRequestsCount;
    }

    private void validateCreateRequest(UserDto user, EventFullDto event) {
        if (user.id() == event.initiator().id()) {
            log.error("User id={} cannot request own event id={}", user.id(), event.id());
            throw new ConflictException("Initiator can't request their own event");
        }
        if (!event.state().equals(EventState.PUBLISHED)) {
            log.error("Event id={} is not published", event.id());
            throw new ConflictException("Event is not published");
        }
        long confirmedCount = requestRepository.countByStatusAndEventId(RequestStatus.CONFIRMED, event.id());
        if (event.participantLimit() > 0 && confirmedCount >= event.participantLimit()) {
            log.error("Participant limit reached for event id={}", event.id());
            throw new ConflictException("No available slots for this event");
        }
    }

    private void validateRequestOwnership(long userId, Request request) {
        if (!request.getRequesterId().equals(userId)) {
            log.error("User id={} is not the owner of request id={}", userId, request.getId());
            throw new AccessException("User does not own this request");
        }
    }

    private void validateEventOwnership(UserDto user, EventFullDto event) {
        if (!event.initiator().id().equals(user.id())) {
            log.error("User id={} is not the initiator of event id={}", user.id(), event.id());
            throw new AccessException("User is not the event initiator");
        }
    }

    private void validateRequestsForUpdate(List<Request> requests, EventFullDto event) {
        for (Request request : requests) {
            if (!request.getStatus().equals(RequestStatus.PENDING)) {
                log.error("Request id={} has status {} instead of PENDING", request.getId(), request.getStatus());
                throw new ConflictException("All requests must be in PENDING status");
            }
        }
    }

    private void setInitialStatus(Request request, EventFullDto event) {
        if (!event.requestModeration() || event.participantLimit() == 0) {
            request.setStatus(RequestStatus.CONFIRMED);
            log.debug("Request set to CONFIRMED due to no moderation or no limit");
        } else {
            request.setStatus(RequestStatus.PENDING);
            log.debug("Request set to PENDING");
        }
    }

    private void updateRequestsStatus(List<Request> requests, RequestStatus newStatus, EventFullDto event, long confirmedCount) {
        for (Request request : requests) {
            if (newStatus == RequestStatus.CONFIRMED && confirmedCount >= event.participantLimit()) {
                log.warn("Participant limit reached, rejecting remaining requests");
                requestRepository.cancelNewRequestsStatus(event.id());
                break;
            }
            requestRepository.updateStatus(newStatus.toString(), request.getId());
            log.debug("Updated request id={} to status={}", request.getId(), newStatus);
            if (newStatus == RequestStatus.CONFIRMED) {
                confirmedCount++;
            }
        }
    }

    private List<ParticipationRequestDto> fetchConfirmedRequests(EventFullDto event) {
        return requestRepository.findAllByStatus(RequestStatus.CONFIRMED).stream()
                .filter(request -> request.getEventId().equals(event.id()))
                .map(requestMapper::toParticipationRequestDto)
                .toList();
    }

    private List<ParticipationRequestDto> fetchRejectedRequests(EventFullDto event) {
        return requestRepository.findAllByStatus(RequestStatus.REJECTED).stream()
                .filter(request -> request.getEventId().equals(event.id()))
                .map(requestMapper::toParticipationRequestDto)
                .toList();
    }

    private Request findRequestById(long requestId) {
        return requestRepository.findById(requestId)
                .orElseThrow(() -> {
                    log.error("Request with id={} not found", requestId);
                    return new NotFoundException("Request with id=" + requestId + " not found");
                });
    }
}