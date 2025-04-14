package ru.practicum.ewm.event.controller;

import ru.practicum.ewm.event.dto.UpdateEventAdminRequest;
import ru.practicum.ewm.event.dto.UpdateEventUserRequest;

public record EventUpdateParams(
        Long userId,
        UpdateEventUserRequest updateEventUserRequest,
        UpdateEventAdminRequest updateEventAdminRequest
) {
}
