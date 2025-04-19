package ru.practicum.service;

import java.util.List;
import java.util.Map;

public interface LikeService {

    Map<Long, Long> getAllEventsLikesByIds(List<Long> eventIdList);

    Long getCountByEventId(Long eventId);

    Long getCountByLocationId(Long locationId);

    Long addEventLike(Long eventId, Long userId);

    Long deleteEventLike(Long eventId, Long userId);

    Long addLocationLike(Long locationId, Long userId);

    Long deleteLocationLike(Long locationId, Long userId);

    Map<Long, Long> getTopLikedLocationsIds(Integer count);

    Map<Long, Long> getTopLikedEventsIds(Integer count);

}
