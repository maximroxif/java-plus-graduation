package ru.practicum.ewm.dto.request;

import ru.practicum.ewm.entity.RequestStatus;

import java.time.LocalDateTime;

public record ParticipationRequestDto(

        LocalDateTime created,

        Long event,

        Long id,

        Long requester,

        RequestStatus status
) {
}
