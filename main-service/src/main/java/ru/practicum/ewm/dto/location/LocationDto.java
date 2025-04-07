package ru.practicum.ewm.dto.location;

public record LocationDto(
        long id,
        Float lat,
        Float lon,
        Long likes
) {
}


