package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.service.LikeService;

import java.util.List;
import java.util.Map;

@Slf4j()
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/likes")
public class InternalLikeController {

    private final LikeService likeService;

    @GetMapping("/events/{eventId}")
    public Long getCountByEventId(@PathVariable Long eventId) {
        log.info("==> GET /internal/likes/events/{eventId} Getting like by eventId: {}", eventId);
        Long returnedLikes = likeService.getCountByEventId(eventId);
        log.info("<== GET /internal/likes/events/{eventId} Returned liked if event: {}, {}", eventId, returnedLikes);
        return returnedLikes;
    }

    @GetMapping("/events")
    public Map<Long, Long> getAllEventsLikesByIds(@RequestParam List<Long> eventIdList) {
        log.info("==> GET /internal/likes Getting likes by eventIdList: {}", eventIdList);
        Map<Long,Long> returnedLikesMap = likeService.getAllEventsLikesByIds(eventIdList);
        log.info("<== GET /internal/likes Returned likeList: {}", returnedLikesMap);
        return returnedLikesMap;
    }


    @GetMapping("/locations/{locationId}")
    public Long getCountByLocationId(@PathVariable Long locationId) {
        log.info("==> GET /internal/likes/locations/{locationId} Getting like by locationId: {}", locationId);
        Long returnedLikes = likeService.getCountByLocationId(locationId);
        log.info("<== GET /internal/likes/locations/{locationId} Returned liked by locationId: {}, {}",
                locationId, returnedLikes);
        return returnedLikes;
    }

    @GetMapping("/locations/top")
    Map<Long, Long> getTopLikedLocationsIds(@RequestParam(required = false, defaultValue = "10") Integer count) {
        log.info("==> GET /internal/likes/locations/top Getting top-liked location ids");
        Map<Long, Long> topLikedLocationsIds = likeService.getTopLikedLocationsIds(count);
        log.info("<== GET /internal/likes/locations/top Returning top-liked locations : {}", topLikedLocationsIds);
        return topLikedLocationsIds;
    }

    @GetMapping("/events/top")
    Map<Long, Long> getTopLikedEventsIds(@RequestParam(required = false, defaultValue = "10") Integer count) {
        log.info("==> GET /internal/likes/events/top Getting top-liked events ids");
        Map<Long, Long> topLikedEventsIds = likeService.getTopLikedEventsIds(count);
        log.info("<== GET /internal/likes/events/top Returning top-liked events: {}", topLikedEventsIds);
        return topLikedEventsIds;
    }
}
