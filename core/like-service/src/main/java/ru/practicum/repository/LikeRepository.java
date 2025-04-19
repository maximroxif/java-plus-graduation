package ru.practicum.repository;


import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface LikeRepository {

    //EVENTS
    Long addEventLike(long userId, long eventId);

    Long deleteEventLike(long userId, long eventId);

    Long getCountByEventId(long eventId);

    Long getCountByLocationId(long locationId);

    Map<Long, Long> getAllEventsLikesByIds(List<Long> eventIds);

    Map<Long, Long> getTopLikedEventsIds(Integer count);

    //LOCATIONS
    Long addLocationLike(Long locationId, Long userId);

    Long deleteLocationLike(Long locationId, Long userId);

    Map<Long, Long> getTopLikedLocationsIds(Integer count);

    Optional<Boolean> isEventLiked(long eventId, long userId);

    Optional<Boolean> isLocationLiked(long locationId, long userId);


}
