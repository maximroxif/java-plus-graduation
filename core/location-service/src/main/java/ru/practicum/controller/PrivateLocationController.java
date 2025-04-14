package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.dto.location.LocationDto;
import ru.practicum.service.LocationService;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/users/{userId}/locations")
public class PrivateLocationController {

    private final LocationService locationService;

    @GetMapping("/top")
    public List<LocationDto> getTop(
            @PathVariable long userId,
            @RequestParam(required = false, defaultValue = "10") Integer count) {
        log.info("==> GET /users/{userId}/locations/top");

        List<LocationDto> locationDtoList = locationService.getTop(userId, count);
        log.info("<== GET /users/{userId}/locations/top Returning top {} locations.", count);
        return locationDtoList;
    }

    @PostMapping
    public LocationDto create(@PathVariable long userId,
                              @RequestBody LocationDto location) {
        log.info("==> POST /users/{userId}/locations");
        LocationDto savedLocation = locationService.create(location);
        log.info("<== POST /users/{userId}/locations Location created.");
        return savedLocation;
    }








}



