package ru.practicum.ewm.request.controller;

import ru.practicum.ewm.request.dto.EventRequestStatusUpdateRequest;

public record PrivateUpdateRequestParams(
        long userId,
        long eventId,
        EventRequestStatusUpdateRequest eventRequestStatusUpdateRequest
) {
}
