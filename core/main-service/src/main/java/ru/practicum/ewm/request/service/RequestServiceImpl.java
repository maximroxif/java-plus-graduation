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
import java.util.Objects;

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

        userRepository.findById(userId)
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
        User user = userRepository.findById(params.userId())
                .orElseThrow(() -> new NotFoundException("User with id " + params.userId() + " not found"));
        Event event = eventRepository.findById(params.eventId())
                .orElseThrow(() -> new NotFoundException("Event with id " + params.eventId() + " not found"));

        if (!Objects.equals(event.getInitiator().getId(), user.getId())) {
            throw new AccessException("User with id " + params.userId() + " is not own event");
        }

        List<Request> requestListOfEvent =
                requestRepository.findAllByIdInAndEventId(
                        params.eventRequestStatusUpdateRequest().requestIds(), params.eventId());

        long confirmedRequestsCount =
                requestRepository.countByStatusAndEventId(RequestStatus.CONFIRMED, params.eventId());


        for (Request request : requestListOfEvent) {
            if (request.getStatus() != RequestStatus.PENDING) {
                throw new ConflictException("Request status is not PENDING");
            }

            if (confirmedRequestsCount >= event.getParticipantLimit()) {
                throw new ConflictException("Participant limit exceeded");
            }

            if (event.isRequestModeration()) {
                String status = params.eventRequestStatusUpdateRequest().status().toString();
                log.debug("State for update: {}", status);
                requestRepository.updateStatus(
                        status, request.getId());
                Request modifiedRequest = requestRepository.findById(request.getId())
                        .orElseThrow(() -> new NotFoundException("Request with id " + request.getId() + " not found"));
                log.debug("Updated {} {}", modifiedRequest.getId(), modifiedRequest.getStatus());
                if (params.eventRequestStatusUpdateRequest().status() == RequestStatus.CONFIRMED) {
                    confirmedRequestsCount++;
                }
                if (confirmedRequestsCount >= event.getParticipantLimit()) {
                    requestRepository.cancelNewRequestsStatus(event.getId());
                }
            }
        }

        List<ParticipationRequestDto> confirmedRequestsDtoList =
                requestRepository.findAllByStatus(RequestStatus.CONFIRMED)
                        .stream()
                        .filter(request -> request.getEvent().getId() == event.getId())
                        .map(requestMapper::toParticipationRequestDto)
                        .toList();
        List<Request> rejectedRequests = requestRepository.findAllByStatus(RequestStatus.REJECTED);
        for (Request request : rejectedRequests) {
            log.debug("{} id, status: {}", request.getId(), request.getStatus());
        }

        List<ParticipationRequestDto> rejectedRequestsDtoList =
                rejectedRequests
                        .stream()
                        .filter(request -> request.getEvent().getId() == event.getId())
                        .map(requestMapper::toParticipationRequestDto)
                        .toList();

        return new EventRequestStatusUpdateResult(confirmedRequestsDtoList, rejectedRequestsDtoList);
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

    private void setInitialStatus(Request request, Event event) {
        if (!event.isRequestModeration() || event.getParticipantLimit() == 0) {
            request.setStatus(RequestStatus.CONFIRMED);
            log.debug("Request set to CONFIRMED due to no moderation or no limit");
        } else {
            request.setStatus(RequestStatus.PENDING);
            log.debug("Request set to PENDING");
        }
    }
}