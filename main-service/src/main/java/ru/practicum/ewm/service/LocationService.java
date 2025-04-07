package ru.practicum.ewm.service;

import ru.practicum.ewm.dto.location.LocationDto;

import java.util.List;

public interface LocationService {

    LocationDto addLike(long userId, long locationId);

    void deleteLike(long userId, long locationId);

    List<LocationDto> getTop(long userId, Integer count);
}
