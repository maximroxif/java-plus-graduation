package ru.practicum.ewm.request.dto;

import jakarta.validation.constraints.NotNull;
import ru.practicum.ewm.request.model.RequestStatus;

import java.util.List;

public record EventRequestStatusUpdateRequest(

        List<Long> requestIds,

        @NotNull
        RequestStatus status

) {
}
