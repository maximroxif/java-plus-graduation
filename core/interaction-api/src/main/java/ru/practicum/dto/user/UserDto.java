package ru.practicum.dto.user;

public record UserDto(
        String email,
        long id,
        String name
) {
}
