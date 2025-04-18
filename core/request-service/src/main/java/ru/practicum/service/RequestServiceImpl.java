package ru.practicum.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequestServiceImpl implements RequestService {

    private final RequestRepository requestRepository;
    private final UserServiceClient userServiceClient;
    private final EventServiceClient eventServiceClient;
    private final RequestMapper requestMapper;

    @Override
    public ParticipationRequestDto create(long userId, long eventId) {
        log.info("Creating participation request for userId={} and eventId={}", userId, eventId);

        UserDto user = userServiceClient.getById(userId);
        EventFullDto event = eventServiceClient.getById(eventId);
        log.debug("Retrieved user: {}, event: {}", user, event);

        long confirmedRequests = requestRepository.countByStatusAndEventId(RequestStatus.CONFIRMED, eventId);
        log.debug("Confirmed requests count for eventId={}: {}", eventId, confirmedRequests);

        if (userId == event.initiator().id()) {
            log.error("UserId={} is the initiator of eventId={}. Cannot create request.", userId, eventId);
            throw new ConflictException("Initiator can't request their own event");
        }
        if (!EventState.PUBLISHED.equals(event.state())) {
            log.error("EventId={} is not published. Current state: {}", eventId, event.state());
            throw new ConflictException("Event is not published");
        }
        if (event.participantLimit() != 0 && event.participantLimit() <= confirmedRequests) {
            log.error("EventId={} has no available slots. Limit: {}, Confirmed: {}",
                    eventId, event.participantLimit(), confirmedRequests);
            throw new ConflictException("No available slots for this event");
        }

        Request request = requestMapper.toRequest(user, event);
        log.debug("Created request object: {}", request);

        if (!event.requestModeration() || event.participantLimit() == 0) {
            request.setStatus(RequestStatus.CONFIRMED);
            log.info("Request for eventId={} set to CONFIRMED (no moderation or no limit)", eventId);
        } else {
            request.setStatus(RequestStatus.PENDING);
            log.info("Request for eventId={} set to PENDING (requires moderation)", eventId);
        }

        Request savedRequest = requestRepository.save(request);
        log.info("Request saved successfully: id={}", savedRequest.getId());

        return requestMapper.toParticipationRequestDto(savedRequest);
    }

    @Override
    public List<ParticipationRequestDto> getAllOwnRequests(long userId) {
        log.info("Fetching all participation requests for userId={}", userId);

        userServiceClient.checkExistence(userId);
        log.debug("User with id={} exists", userId);

        List<Request> requests = requestRepository.getAllByRequesterId(userId);
        log.debug("Retrieved {} requests for userId={}", requests.size(), userId);

        return requests.stream()
                .map(requestMapper::toParticipationRequestDto)
                .toList();
    }

    @Override
    public ParticipationRequestDto cancel(long userId, long requestId) {
        log.info("Canceling requestId={} for userId={}", requestId, userId);

        userServiceClient.checkExistence(userId);
        log.debug("User with id={} exists", userId);

        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> {
                    log.error("RequestId={} not found", requestId);
                    return new NotFoundException("Request with id " + requestId + " not found");
                });

        request.setStatus(RequestStatus.CANCELED);
        log.debug("RequestId={} status set to CANCELED", requestId);

        Request updatedRequest = requestRepository.save(request);
        log.info("RequestId={} canceled successfully", requestId);

        return requestMapper.toParticipationRequestDto(updatedRequest);
    }

    @Override
    public List<ParticipationRequestDto> getAllForOwnEvent(long userId, long eventId) {
        log.info("Fetching all requests for eventId={} by userId={}", eventId, userId);

        UserDto user = userServiceClient.getById(userId);
        EventFullDto event = eventServiceClient.getById(eventId);
        log.debug("Retrieved user: {}, event: {}", user, event);

        if (!Objects.equals(event.initiator().id(), user.id())) {
            log.error("UserId={} is not the initiator of eventId={}", userId, eventId);
            throw new AccessException("User with id " + userId + " does not own the event");
        }

        List<Request> requests = requestRepository.getAllByEventId(eventId);
        log.debug("Retrieved {} requests for eventId={}", requests.size(), eventId);

        return requests.stream()
                .map(requestMapper::toParticipationRequestDto)
                .toList();
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateStatus(PrivateUpdateRequestParams params) {
        log.info("Updating request statuses for userId={}, eventId={}", params.userId(), params.eventId());

        UserDto user = userServiceClient.getById(params.userId());
        EventFullDto event = eventServiceClient.getById(params.eventId());
        log.debug("Retrieved user: {}, event: {}", user, event);

        if (!Objects.equals(event.initiator().id(), user.id())) {
            log.error("UserId={} is not the initiator of eventId={}", params.userId(), params.eventId());
            throw new AccessException("User with id " + params.userId() + " does not own the event");
        }

        List<Long> requestIds = params.eventRequestStatusUpdateRequest().requestIds();
        List<Request> requests = requestRepository.findAllByIdInAndEventId(requestIds, params.eventId());
        log.debug("Retrieved {} requests for eventId={}", requests.size(), params.eventId());

        long confirmedRequestsCount = requestRepository.countByStatusAndEventId(RequestStatus.CONFIRMED, params.eventId());
        log.debug("Confirmed requests count for eventId={}: {}", params.eventId(), confirmedRequestsCount);

        for (Request request : requests) {
            if (!RequestStatus.PENDING.equals(request.getStatus())) {
                log.error("RequestId={} is not in PENDING status. Current status: {}", request.getId(), request.getStatus());
                throw new ConflictException("Request status is not PENDING");
            }

            if (event.participantLimit() > 0 && confirmedRequestsCount >= event.participantLimit()) {
                log.error("Participant limit exceeded for eventId={}. Limit: {}, Confirmed: {}",
                        params.eventId(), event.participantLimit(), confirmedRequestsCount);
                throw new ConflictException("Participant limit exceeded");
            }

            if (event.requestModeration()) {
                String newStatus = params.eventRequestStatusUpdateRequest().status().toString();
                log.debug("Updating requestId={} to status={}", request.getId(), newStatus);

                requestRepository.updateStatus(newStatus, request.getId());
                Request updatedRequest = requestRepository.findById(request.getId())
                        .orElseThrow(() -> {
                            log.error("RequestId={} not found after update", request.getId());
                            return new NotFoundException("Request with id " + request.getId() + " not found");
                        });

                log.info("RequestId={} updated to status={}", updatedRequest.getId(), updatedRequest.getStatus());

                if (RequestStatus.CONFIRMED.equals(params.eventRequestStatusUpdateRequest().status())) {
                    confirmedRequestsCount++;
                    log.debug("Incremented confirmed requests count: {}", confirmedRequestsCount);
                }

                if (event.participantLimit() > 0 && confirmedRequestsCount >= event.participantLimit()) {
                    log.info("Participant limit reached for eventId={}. Canceling remaining requests.", params.eventId());
                    requestRepository.cancelNewRequestsStatus(event.id());
                }
            }
        }

        List<ParticipationRequestDto> confirmedRequests = requestRepository.findAllByStatus(RequestStatus.CONFIRMED)
                .stream()
                .filter(request -> request.getEventId().equals(event.id()))
                .map(requestMapper::toParticipationRequestDto)
                .toList();
        log.debug("Retrieved {} confirmed requests for eventId={}", confirmedRequests.size(), event.id());

        List<ParticipationRequestDto> rejectedRequests = requestRepository.findAllByStatus(RequestStatus.REJECTED)
                .stream()
                .filter(request -> request.getEventId().equals(event.id()))
                .map(requestMapper::toParticipationRequestDto)
                .toList();
        log.debug("Retrieved {} rejected requests for eventId={}", rejectedRequests.size(), event.id());

        EventRequestStatusUpdateResult result = new EventRequestStatusUpdateResult(confirmedRequests, rejectedRequests);
        log.info("Status update completed for eventId={}. Confirmed: {}, Rejected: {}",
                params.eventId(), confirmedRequests.size(), rejectedRequests.size());

        return result;
    }

    @Override
    public long countByStatusAndEventId(RequestStatus status, long eventId) {
        log.info("Counting requests with status={} for eventId={}", status, eventId);

        long count = requestRepository.countByStatusAndEventId(status, eventId);
        log.debug("Count of requests with status={} for eventId={}: {}", status, eventId, count);

        return count;
    }

    @Override
    public Map<Long, Long> countByStatusAndEventsIds(RequestStatus status, List<Long> eventIds) {
        log.info("Counting requests with status={} for eventIds={}", status, eventIds);

        List<Map<String, Long>> results = requestRepository.countByStatusAndEventsIds(status.toString(), eventIds);
        Map<Long, Long> eventRequestsCount = new HashMap<>();

        for (Map<String, Long> row : results) {
            Long eventId = row.get("EVENT_ID");
            Long count = row.get("EVENT_COUNT");
            eventRequestsCount.put(eventId, count);
            log.debug("EventId={} has {} requests with status={}", eventId, count, status);
        }

        log.info("Completed counting requests for {} events", eventIds.size());
        return eventRequestsCount;
    }
}