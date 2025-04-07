package ru.practicum.ewm.location.service;

import ru.practicum.ewm.location.dto.LocationDto;

import java.util.List;

public interface LocationService {

    LocationDto addLike(long userId, long locationId);

    void deleteLike(long userId, long locationId);

    List<LocationDto> getTop(long userId, Integer count);
}
