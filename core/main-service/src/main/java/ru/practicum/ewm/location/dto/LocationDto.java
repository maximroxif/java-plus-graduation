package ru.practicum.ewm.location.dto;

public record LocationDto(
        long id,
        Float lat,
        Float lon,
        Long likes
) {
}


