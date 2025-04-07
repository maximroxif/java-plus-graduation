package ru.practicum.ewm.dto.request;

import jakarta.validation.constraints.NotNull;
import ru.practicum.ewm.entity.RequestStatus;

import java.util.List;

public record EventRequestStatusUpdateRequest(

        List<Long> requestIds,

        @NotNull
        RequestStatus status

) {
}
