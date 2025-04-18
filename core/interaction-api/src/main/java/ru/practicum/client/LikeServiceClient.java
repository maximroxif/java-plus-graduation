package ru.practicum.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(name = "like-service")
public interface LikeServiceClient {

    //<================================== EVENT LIKE==============================================>

    @GetMapping("/internal/likes/events/{eventId}")
        //Получение количества лайков события по его id
    Long getCountByEventId(@PathVariable Long eventId);

    @PutMapping("/users/{userId}/events/{eventId}/likes")
        //Добавление лайка события
    Long addEventLike(@PathVariable long userId, @PathVariable long eventId);

    @DeleteMapping("/users/{userId}/events//{eventId}/likes")
        //удаление лайка события
    Long deleteEventLike(@PathVariable long userId, @PathVariable long eventId);

    @GetMapping("/internal/likes/events")
        //Получение количества лайков событий по списку id
    Map<Long, Long> getAllEventsLikesByIds(@RequestParam List<Long> eventIdList);

    //<================================== TOP LIKED==============================================>

    @GetMapping("/internal/likes/locations/top")
        //Получение id топ-локаций по лайкам
    Map<Long, Long> getTopLikedLocationsIds(
            @RequestParam(required = false, defaultValue = "10") Integer count);

    @GetMapping("/internal/likes/events/top")
        //Получение id топ-событий по лайкам
    Map<Long, Long> getTopLikedEventsIds(@RequestParam(required = false, defaultValue = "10") Integer count);

    @GetMapping("/internal/likes/locations/{locationId}")
    Long getCountByLocationId(@PathVariable Long locationId);

}
