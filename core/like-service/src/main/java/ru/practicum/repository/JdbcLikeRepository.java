package ru.practicum.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JdbcLikeRepository implements LikeRepository {
    private final NamedParameterJdbcOperations jdbc;

    //EVENT LIKES

    @Override
    public Long addEventLike(long userId, long eventId) {
        String sql = "INSERT into likes_events (event_id, user_id) values (:eventId, :userId)";
        jdbc.update(sql, Map.of("eventId", eventId, "userId", userId));
        return getCountByEventId(eventId);
    }

    @Override
    public Long deleteEventLike(long userId, long eventId) {
        String sql = "DELETE FROM LIKES_EVENTS WHERE EVENT_ID = :eventId AND USER_ID = :userId";
        jdbc.update(sql, Map.of("eventId", eventId, "userId", userId));
        return getCountByEventId(eventId);
    }

    @Override
    public Long getCountByEventId(long eventId) {
        String sql = "select count(*) from likes_events where event_id = :eventId";
        return jdbc.queryForObject(sql, Map.of("eventId", eventId), Long.class);
    }

    public Map<Long, Long> getAllEventsLikesByIds(List<Long> eventIds) {
        String sql = """
                select EVENT_ID, COUNT(*) as EVENT_COUNT
                from likes_events where event_id in ( :eventIds )
                GROUP BY EVENT_ID
                """;
        List<Map<String, Object>> rows = jdbc.queryForList(sql, Map.of("eventIds", eventIds));

        Map<Long, Long> eventLikes = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Long eventId = (Long) row.get("EVENT_ID");
            Long likesCount = (Long) row.get("EVENT_COUNT");
            eventLikes.put(eventId, likesCount);
        }
        return eventLikes;
    }

    @Override
    public Map<Long, Long> getTopLikedEventsIds(Integer count) {
        String sql = """
                        SELECT EVENT_ID, COUNT(*) AS EVENT_LIKES FROM LIKES_EVENTS
                        GROUP BY EVENT_ID
                        ORDER BY EVENT_LIKES DESC NULLS LAST
                        LIMIT :count
                """;
        List<Map<String, Object>> rows = jdbc.queryForList(sql, Map.of("count", count));

        Map<Long, Long> eventsLikes = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Long eventId = (Long) row.get("EVENT_ID");
            Long likesCount = (Long) row.get("EVENT_LIKES");
            eventsLikes.put(likesCount, eventId);
        }
        return eventsLikes;
    }

    //LOCATIONS LIKES

    @Override
    public Long addLocationLike(Long locationId, Long userId) {
        String sql = "merge into likes_locations (location_id, user_id) values (:locationId, :userId)";
        jdbc.update(sql, Map.of("locationId", locationId, "userId", userId));
        return getCountByLocationId(locationId);
    }

    @Override
    public Long deleteLocationLike(Long locationId, Long userId) {
        String sql = "DELETE FROM LIKES_LOCATIONS WHERE LOCATION_ID = :locationId AND USER_ID = :userId";
        jdbc.update(sql, Map.of("locationId", locationId, "userId", userId));
        return getCountByLocationId(locationId);
    }

    @Override
    public Map<Long, Long> getTopLikedLocationsIds(Integer count) {
        String sql = """
                        SELECT LOCATION_ID, COUNT(*) AS LOCATION_LIKES FROM LIKES_LOCATIONS
                        GROUP BY LOCATION_ID
                        ORDER BY LOCATION_LIKES DESC NULLS LAST
                        LIMIT :count
                """;
        List<Map<String, Object>> rows = jdbc.queryForList(sql, Map.of("count", count));

        Map<Long, Long> locationsLikes = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Long locationId = (Long) row.get("LOCATION_ID");
            Long likesCount = (Long) row.get("LOCATION_LIKES");
            locationsLikes.put(locationId, likesCount);
        }
        return locationsLikes;
    }

    @Override
    public Optional<Boolean> isEventLiked(long eventId, long userId) {
        String sql = """
                SELECT EXISTS(SELECT * FROM LIKES_EVENTS WHERE EVENT_ID = :eventId AND USER_ID = :userId)
                """;
        return Optional.ofNullable(
                jdbc.queryForObject(sql, Map.of("eventId", eventId, "userId", userId), Boolean.class));
    }

    @Override
    public Optional<Boolean> isLocationLiked(long locationId, long userId) {
        String sql = """
                SELECT EXISTS(SELECT * FROM LIKES_LOCATIONS WHERE LOCATION_ID = :locationId AND USER_ID = :userId)
                """;
        return Optional.ofNullable(
                jdbc.queryForObject(sql, Map.of("locationId", locationId, "userId", userId), Boolean.class));
    }

    @Override
    public Long getCountByLocationId(long locationId) {
        String sql = "select count(*) from likes_locations where location_id = :locationId";
        return jdbc.queryForObject(sql, Map.of("locationId", locationId), Long.class);
    }


}
