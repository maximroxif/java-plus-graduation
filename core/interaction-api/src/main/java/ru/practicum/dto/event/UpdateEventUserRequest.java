package ru.practicum.dto.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import ru.practicum.constant.Constants;
import ru.practicum.dto.event.annotation.FutureAfterTwoHours;
import ru.practicum.dto.location.LocationDto;
import ru.practicum.enums.StateAction;

import java.time.LocalDateTime;

public record UpdateEventUserRequest(

        @Size(min = 20, max = 2000)
        String annotation,

        Long category,

        @Size(min = 20, max = 7000)
        String description,

        @JsonFormat(pattern = Constants.JSON_TIME_FORMAT, shape = JsonFormat.Shape.STRING)
        @FutureAfterTwoHours
        LocalDateTime eventDate,

        LocationDto location,

        Boolean paid,

        @PositiveOrZero
        Integer participantLimit,

        String publishedOn,

        Boolean requestModeration,

        StateAction stateAction,

        @Size(min = 3, max = 120)
        String title

) {
}
