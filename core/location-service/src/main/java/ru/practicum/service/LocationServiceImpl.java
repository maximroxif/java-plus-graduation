package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.client.LikeServiceClient;
import ru.practicum.client.UserServiceClient;
import ru.practicum.dto.location.LocationDto;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.LocationMapper;
import ru.practicum.model.Location;
import ru.practicum.repository.LocationRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LocationServiceImpl implements LocationService {

    private final LocationRepository locationRepository;
    private final UserServiceClient userServiceClient;
    private final LocationMapper locationMapper;
    private final LikeServiceClient likeServiceClient;

    @Override
    @Transactional(readOnly = true)
    public List<LocationDto> getTop(long userId, Integer count) {
        log.info("Fetching top {} locations for userId={}", count, userId);

        userServiceClient.checkExistence(userId);
        if (count == null || count <= 0) {
            log.warn("Invalid count value: {}, returning empty list", count);
            return List.of();
        }

        Map<Long, Long> topLocationIds = likeServiceClient.getTopLikedLocationsIds(count);
        log.debug("Retrieved {} top location IDs from LikeService", topLocationIds.size());

        List<Location> locations = locationRepository.findAllById(topLocationIds.keySet());
        locations.forEach(location -> {
            location.setLikes(topLocationIds.getOrDefault(location.getId(), 0L));
            log.debug("Location id={} has {} likes", location.getId(), location.getLikes());
        });

        log.info("Returning {} top locations", locations.size());
        return locations.stream()
                .map(locationMapper::locationToLocationDto)
                .toList();
    }

    @Override
    @Transactional
    public LocationDto create(LocationDto locationDto) {
        log.info("Creating new location: {}", locationDto);

        if (locationDto == null) {
            log.error("LocationDto is null");
            throw new IllegalArgumentException("LocationDto cannot be null");
        }

        Location location = locationMapper.locationDtoToLocation(locationDto);
        Location savedLocation = locationRepository.save(location);
        log.info("Location successfully created with id={}", savedLocation.getId());

        return locationMapper.locationToLocationDto(savedLocation);
    }

    @Override
    @Transactional(readOnly = true)
    public LocationDto getById(long locationId) {
        log.info("Fetching location with id={}", locationId);

        Location location = findLocationById(locationId);
        log.debug("Location id={} found", locationId);

        return locationMapper.locationToLocationDto(location);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, LocationDto> getAllById(List<Long> locationIds) {
        log.info("Fetching locations with ids={}", locationIds);

        if (locationIds == null || locationIds.isEmpty()) {
            log.warn("Empty or null locationIds provided, returning empty map");
            return Map.of();
        }

        List<Location> locations = locationRepository.findAllById(locationIds);
        log.info("Found {} locations", locations.size());

        Map<Long, LocationDto> locationDtoMap = locations.stream()
                .collect(Collectors.toMap(
                        Location::getId,
                        locationMapper::locationToLocationDto
                ));

        return locationDtoMap;
    }

    private Location findLocationById(long locationId) {
        return locationRepository.findById(locationId)
                .orElseThrow(() -> {
                    log.error("Location with id={} not found", locationId);
                    return new NotFoundException("Location with id=" + locationId + " not found");
                });
    }
}