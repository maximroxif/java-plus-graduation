package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.dto.location.LocationDto;
import ru.practicum.service.LocationService;

import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/internal/locations")
public class InternalLocationController {
    private final LocationService locationService;

    @GetMapping("/{locationId}")
    public LocationDto getById(@PathVariable long locationId) {
        log.info("==> GET /internal/locations/{locationId} Getting location with id: {}", locationId);
        LocationDto location = locationService.getById(locationId);
        log.info("<== GET /internal/locations/{locationId} Returning location with id: {}", location);
        return location;
    }

    @GetMapping("/all")
    public Map<Long, LocationDto> getAllById(@RequestParam List<Long> locationIds) {
        log.info("==> GET /internal/locations/ Getting Locations with ids: {}", locationIds);
        Map<Long, LocationDto> locations = locationService.getAllById(locationIds);
        log.info("<== GET /internal/locations/ Returning locations: {}", locations);
        return locations;
    }

}
