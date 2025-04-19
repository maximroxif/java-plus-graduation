package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.client.EventServiceClient;
import ru.practicum.client.LocationServiceClient;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.location.LocationDto;
import ru.practicum.enums.EventState;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.repository.LikeRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class LikeServiceImpl implements LikeService {

    private final LikeRepository likeRepository;
    private final EventServiceClient eventServiceClient;
    private final LocationServiceClient locationServiceClient;

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Long> getAllEventsLikesByIds(List<Long> eventIds) {
        log.info("Fetching likes for eventIds={}", eventIds);

        if (eventIds == null || eventIds.isEmpty()) {
            log.warn("Empty or null eventIds provided, returning empty map");
            return Map.of();
        }

        Map<Long, Long> likes = likeRepository.getAllEventsLikesByIds(eventIds);
        log.info("Found likes for {} events", likes.size());
        return likes;
    }

    @Override
    @Transactional(readOnly = true)
    public Long getCountByEventId(Long eventId) {
        log.info("Counting likes for eventId={}", eventId);

        if (eventId == null) {
            log.error("EventId is null");
            throw new IllegalArgumentException("EventId cannot be null");
        }

        Long count = likeRepository.getCountByEventId(eventId);
        log.debug("Found {} likes for eventId={}", count, eventId);
        return count;
    }

    @Override
    @Transactional(readOnly = true)
    public Long getCountByLocationId(Long locationId) {
        log.info("Counting likes for locationId={}", locationId);

        if (locationId == null) {
            log.error("LocationId is null");
            throw new IllegalArgumentException("LocationId cannot be null");
        }

        Long count = likeRepository.getCountByLocationId(locationId);
        log.debug("Found {} likes for locationId={}", count, locationId);
        return count;
    }

    @Override
    @Transactional
    public Long addEventLike(Long eventId, Long userId) {
        log.info("Adding like from userId={} to eventId={}", userId, eventId);

        if (eventId == null || userId == null) {
            log.error("EventId or userId is null: eventId={}, userId={}", eventId, userId);
            throw new IllegalArgumentException("EventId and userId cannot be null");
        }

        EventFullDto event = eventServiceClient.getById(eventId);
        log.debug("Retrieved event: {}", event);

        if (!event.state().equals(EventState.PUBLISHED)) {
            log.error("Event id={} is not published", eventId);
            throw new ConflictException("Event with id=" + eventId + " is not published");
        }

        Long result = likeRepository.addEventLike(userId, eventId);
        log.info("Like added successfully for eventId={}", eventId);
        return result;
    }

    @Override
    @Transactional
    public Long deleteEventLike(Long eventId, Long userId) {
        log.info("Deleting like from userId={} for eventId={}", userId, eventId);

        if (eventId == null || userId == null) {
            log.error("EventId or userId is null: eventId={}, userId={}", eventId, userId);
            throw new IllegalArgumentException("EventId and userId cannot be null");
        }

        Optional<Boolean> isLiked = likeRepository.isEventLiked(eventId, userId);
        if (isLiked.isEmpty() || !isLiked.get()) {
            log.error("Like from userId={} for eventId={} not found", userId, eventId);
            throw new NotFoundException("Like from user id=" + userId + " for event id=" + eventId + " not found");
        }

        Long result = likeRepository.deleteEventLike(eventId, userId);
        log.info("Like deleted successfully for eventId={}", eventId);
        return result;
    }

    @Override
    @Transactional
    public Long addLocationLike(Long locationId, Long userId) {
        log.info("Adding like from userId={} to locationId={}", userId, locationId);

        if (locationId == null || userId == null) {
            log.error("LocationId or userId is null: locationId={}, userId={}", locationId, userId);
            throw new IllegalArgumentException("LocationId and userId cannot be null");
        }

        LocationDto location = locationServiceClient.getById(locationId);
        log.debug("Retrieved location: {}", location);

        Long result = likeRepository.addLocationLike(locationId, userId);
        log.info("Like added successfully for locationId={}", locationId);
        return result;
    }

    @Override
    @Transactional
    public Long deleteLocationLike(Long locationId, Long userId) {
        log.info("Deleting like from userId={} for locationId={}", userId, locationId);

        if (locationId == null || userId == null) {
            log.error("LocationId or userId is null: locationId={}, userId={}", locationId, userId);
            throw new IllegalArgumentException("LocationId and userId cannot be null");
        }

        Optional<Boolean> isLiked = likeRepository.isLocationLiked(locationId, userId);
        if (isLiked.isEmpty() || !isLiked.get()) {
            log.error("Like from userId={} for locationId={} not found", userId, locationId);
            throw new NotFoundException("Like from user id=" + userId + " for location id=" + locationId + " not found");
        }

        Long result = likeRepository.deleteLocationLike(locationId, userId); // Исправлено: deleteLocationLike вместо deleteEventLike
        log.info("Like deleted successfully for locationId={}", locationId);
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Long> getTopLikedLocationsIds(Integer count) {
        log.info("Fetching top {} liked location IDs", count);

        if (count == null || count <= 0) {
            log.warn("Invalid count value: {}, returning empty map", count);
            return Map.of();
        }

        Map<Long, Long> topLocations = likeRepository.getTopLikedLocationsIds(count);
        log.info("Found {} top liked locations", topLocations.size());
        return topLocations;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Long> getTopLikedEventsIds(Integer count) {
        log.info("Fetching top {} liked event IDs", count);

        if (count == null || count <= 0) {
            log.warn("Invalid count value: {}, returning empty map", count);
            return Map.of();
        }

        Map<Long, Long> topEvents = likeRepository.getTopLikedEventsIds(count);
        log.info("Found {} top liked events", topEvents.size());
        return topEvents;
    }
}