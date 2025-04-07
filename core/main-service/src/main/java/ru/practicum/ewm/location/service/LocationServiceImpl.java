package ru.practicum.ewm.location.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.location.dto.LocationDto;
import ru.practicum.ewm.location.mapper.LocationMapper;
import ru.practicum.ewm.location.model.Location;
import ru.practicum.ewm.location.repository.LocationRepository;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.repository.UserRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LocationServiceImpl implements LocationService {

    private final LocationRepository locationRepository;
    private final UserRepository userRepository;
    private final LocationMapper locationMapper;

    @Override
    @Transactional
    public LocationDto addLike(long userId, long locationId) {
        log.info("Adding like from userId={} to locationId={}", userId, locationId);

        User user = findUserById(userId);
        Location location = findLocationById(locationId);

        locationRepository.addLike(userId, locationId);
        log.debug("Like added successfully for locationId={}", locationId);

        Location updatedLocation = findLocationById(locationId); // Получаем обновлённую локацию
        return locationMapper.locationToLocationDto(updatedLocation);
    }

    @Override
    @Transactional
    public void deleteLike(long userId, long locationId) {
        log.info("Deleting like from userId={} for locationId={}", userId, locationId);

        User user = findUserById(userId);
        Location location = findLocationById(locationId);

        if (!locationRepository.checkLikeExisting(userId, locationId)) {
            log.error("Like from userId={} for locationId={} does not exist", userId, locationId);
            throw new NotFoundException("Like from user id=" + userId + " for location id=" + locationId + " not found");
        }

        locationRepository.deleteLike(userId, locationId);
        log.info("Like deleted successfully for locationId={}", locationId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LocationDto> getTop(long userId, Integer count) {
        log.info("Fetching top {} locations for userId={}", count, userId);

        User user = findUserById(userId);
        List<Location> topLocations = locationRepository.findTop(count);

        if (topLocations.isEmpty()) {
            log.warn("No locations found for top list with count={}", count);
        }

        topLocations.forEach(location -> {
            long likes = locationRepository.countLikesByLocationId(location.getId());
            location.setLikes(likes);
            log.debug("Location id={} has {} likes", location.getId(), likes);
        });

        log.info("Returning {} top locations", topLocations.size());
        return topLocations.stream()
                .map(locationMapper::locationToLocationDto)
                .toList();
    }

    private User findUserById(long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User with id={} not found", userId);
                    return new NotFoundException("User with id=" + userId + " not found");
                });
    }

    private Location findLocationById(long locationId) {
        return locationRepository.findById(locationId)
                .orElseThrow(() -> {
                    log.error("Location with id={} not found", locationId);
                    return new NotFoundException("Location with id=" + locationId + " not found");
                });
    }
}