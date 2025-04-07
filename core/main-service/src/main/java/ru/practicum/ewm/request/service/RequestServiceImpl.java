package ru.practicum.ewm.request.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.EventState;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.exception.AccessException;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.request.controller.PrivateUpdateRequestParams;
import ru.practicum.ewm.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.ewm.request.dto.ParticipationRequestDto;
import ru.practicum.ewm.request.mapper.RequestMapper;
import ru.practicum.ewm.request.model.Request;
import ru.practicum.ewm.request.model.RequestStatus;
import ru.practicum.ewm.request.repository.RequestRepository;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.repository.UserRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequestServiceImpl implements RequestService {

    private final RequestRepository requestRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final RequestMapper requestMapper;

    @Override
    @Transactional
    public ParticipationRequestDto create(long userId, long eventId) {
        log.info("Creating participation request for userId={} and eventId={}", userId, eventId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " not found"));
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " not found"));

        validateCreateRequest(user, event);

        Long confirmedRequests = requestRepository.countByStatusAndEventId(RequestStatus.CONFIRMED, eventId);
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

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " not found"));
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

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " not found"));
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Request with id=" + requestId + " not found"));

        validateRequestOwnership(user, request);

        request.setStatus(RequestStatus.CANCELED);
        Request canceledRequest = requestRepository.save(request);
        log.info("Request id={} successfully canceled", requestId);

        return requestMapper.toParticipationRequestDto(canceledRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParticipationRequestDto> getAllForOwnEvent(long userId, long eventId) {
        log.info("Fetching all requests for eventId={} by userId={}", eventId, userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " not found"));
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " not found"));

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

        User user = userRepository.findById(params.userId())
                .orElseThrow(() -> new NotFoundException("User with id=" + params.userId() + " not found"));
        Event event = eventRepository.findById(params.eventId())
                .orElseThrow(() -> new NotFoundException("Event with id=" + params.eventId() + " not found"));

        validateEventOwnership(user, event);

        List<Long> requestIds = params.eventRequestStatusUpdateRequest().requestIds();
        RequestStatus newStatus = params.eventRequestStatusUpdateRequest().status();
        List<Request> requests = requestRepository.findAllByIdInAndEventId(requestIds, params.eventId());

        validateRequestsForUpdate(requests, event);

        long confirmedCount = requestRepository.countByStatusAndEventId(RequestStatus.CONFIRMED, event.getId());
        log.debug("Current confirmed requests: {}", confirmedCount);

        updateRequestsStatus(requests, newStatus, event, confirmedCount);

        List<ParticipationRequestDto> confirmedRequests = fetchConfirmedRequests(event);
        List<ParticipationRequestDto> rejectedRequests = fetchRejectedRequests(event);

        log.info("Status update completed: {} confirmed, {} rejected", confirmedRequests.size(), rejectedRequests.size());
        return new EventRequestStatusUpdateResult(confirmedRequests, rejectedRequests);
    }

    private void validateCreateRequest(User user, Event event) {
        if (user.getId().equals(event.getInitiator().getId())) {
            log.error("User id={} cannot request own event id={}", user.getId(), event.getId());
            throw new ConflictException("Initiator can't request their own event");
        }
        if (!event.getState().equals(EventState.PUBLISHED)) {
            log.error("Event id={} is not published", event.getId());
            throw new ConflictException("Event is not published");
        }
        long confirmedCount = requestRepository.countByStatusAndEventId(RequestStatus.CONFIRMED, event.getId());
        if (event.getParticipantLimit() > 0 && confirmedCount >= event.getParticipantLimit()) {
            log.error("Participant limit reached for event id={}", event.getId());
            throw new ConflictException("No available slots for this event");
        }
    }

    private void validateRequestOwnership(User user, Request request) {
        if (!request.getRequester().getId().equals(user.getId())) {
            log.error("User id={} is not the owner of request id={}", user.getId(), request.getId());
            throw new AccessException("User does not own this request");
        }
    }

    private void validateEventOwnership(User user, Event event) {
        if (!event.getInitiator().getId().equals(user.getId())) {
            log.error("User id={} is not the initiator of event id={}", user.getId(), event.getId());
            throw new AccessException("User is not the event initiator");
        }
    }

    private void validateRequestsForUpdate(List<Request> requests, Event event) {
        for (Request request : requests) {
            if (!request.getStatus().equals(RequestStatus.PENDING)) {
                log.error("Request id={} has status {} instead of PENDING", request.getId(), request.getStatus());
                throw new ConflictException("All requests must be in PENDING status");
            }
        }
    }

    private void setInitialStatus(Request request, Event event) {
        if (!event.isRequestModeration() || event.getParticipantLimit() == 0) {
            request.setStatus(RequestStatus.CONFIRMED);
            log.debug("Request set to CONFIRMED due to no moderation or no limit");
        } else {
            request.setStatus(RequestStatus.PENDING);
            log.debug("Request set to PENDING");
        }
    }

    private void updateRequestsStatus(List<Request> requests, RequestStatus newStatus, Event event, long confirmedCount) {
        for (Request request : requests) {
            if (newStatus == RequestStatus.CONFIRMED && confirmedCount >= event.getParticipantLimit()) {
                log.warn("Participant limit reached, rejecting remaining requests");
                requestRepository.cancelNewRequestsStatus(event.getId());
                break;
            }
            requestRepository.updateStatus(newStatus.toString(), request.getId());
            log.debug("Updated request id={} to status={}", request.getId(), newStatus);
            if (newStatus == RequestStatus.CONFIRMED) {
                confirmedCount++;
            }
        }
    }

    private List<ParticipationRequestDto> fetchConfirmedRequests(Event event) {
        return requestRepository.findAllByStatus(RequestStatus.CONFIRMED).stream()
                .filter(request -> request.getEvent().getId() == (event.getId()))
                .map(requestMapper::toParticipationRequestDto)
                .toList();
    }

    private List<ParticipationRequestDto> fetchRejectedRequests(Event event) {
        return requestRepository.findAllByStatus(RequestStatus.REJECTED).stream()
                .filter(request -> request.getEvent().getId() == (event.getId()))
                .map(requestMapper::toParticipationRequestDto)
                .toList();
    }
}