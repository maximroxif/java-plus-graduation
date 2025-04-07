package ru.practicum.ewm.event.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import ru.practicum.ewm.config.Constants;
import ru.practicum.ewm.event.dto.annotation.FutureAfterTwoHours;
import ru.practicum.ewm.location.model.Location;
import ru.practicum.ewm.event.model.StateAction;

import java.time.LocalDateTime;

public record UpdateEventAdminRequest(

        @Size(min = 20, max = 2000)
        String annotation,

        Long category,

        @Size(min = 20, max = 7000)
        String description,

        @FutureAfterTwoHours @JsonFormat(pattern = Constants.JSON_TIME_FORMAT)
        LocalDateTime eventDate,

        Location location,

        Boolean paid,

        @PositiveOrZero
        Integer participantLimit,

        Boolean requestModeration,

        StateAction stateAction,

        @Size(min = 3, max = 120)
        String title

) {
}
