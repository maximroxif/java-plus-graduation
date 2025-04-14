package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.service.LikeService;

@Slf4j
@RestController
@RequiredArgsConstructor
public class PrivateLikeController {

    private final LikeService likeService;


    @PutMapping("/users/{userId}/events/{eventId}/likes")
    public Long addEventLike(@PathVariable long userId, @PathVariable long eventId) {
        log.info("==> PUT. /users/{userId}/events/{eventId}/likes" +
                "Adding like for event with id: {} by user with id: {}", eventId, userId);
        Long countOfLikes = likeService.addEventLike(eventId, userId);
        log.info("<== PUT. /users/{userId}/events/{eventId}/likes" +
                "Like for event with id: {} by user with id: {} added. Current count of likes: {}",
                eventId, userId, countOfLikes);
        return countOfLikes;
    }

    @DeleteMapping("/users/{userId}/events/{eventId}/likes")
    public Long deleteEventLike(@PathVariable long userId, @PathVariable long eventId) {
        log.info("==> DELETE. /users/{userId}/events/{eventId}/likes" +
                " Deleting like for event with id: {} by user with id: {}", userId, eventId);
        Long countOfLikes = likeService.deleteEventLike(eventId, userId);
        log.info("<== DELETE. /users/{userId}/events/{eventId}/likes" +
                "Like for event with id: {} by user with id: {} deleted. Current count of likes: {}",
                eventId, userId, countOfLikes);
        return countOfLikes;
    }

    @PutMapping("/users/{userId}/locations/{locationId}/likes")
    public Long addLocationLike(@PathVariable long userId, @PathVariable long locationId) {
        log.info("==> PUT. /users/{userId}/locations/{locationId}/likes" +
                "Adding like for location with id: {} by user with id: {}", locationId, userId);
        Long locationLikesCount = likeService.addLocationLike(locationId, userId);
        log.info("<== PUT. /users/{userId}/locations/{locationId}/likes" +
                "LikeCounts for location with id: {} by user with id: {} added. Current count: {}",
                locationId, userId, locationLikesCount);
        return locationLikesCount;
    }

    @DeleteMapping("/users/{userId}/locations/{locationId}/likes")
    public Long deleteLocationLike(@PathVariable long userId, @PathVariable long locationId) {
        log.info("==> DELETE. /users/{userId}/locations/{locationId}/likes" +
                "Deleting like for location with id: {} by user with id: {}", locationId, userId);
        Long locationLikesCount = likeService.deleteLocationLike(locationId, userId);
        log.info("<== DELETE. /users/{userId}/locations/{locationId}/likes" +
                "Like for location with id: {} by user with id: {} deleted. Current count: {}",
                locationId, userId, locationLikesCount);
        return locationLikesCount;
    }






}
