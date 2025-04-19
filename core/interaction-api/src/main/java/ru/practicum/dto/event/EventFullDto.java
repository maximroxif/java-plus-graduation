package ru.practicum.dto.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import ru.practicum.constant.Constants;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.dto.location.LocationDto;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.enums.EventState;

import java.time.LocalDateTime;

public record EventFullDto(

        String annotation,

        CategoryDto category,

        long confirmedRequests,

        @JsonFormat(pattern = Constants.JSON_TIME_FORMAT, shape = JsonFormat.Shape.STRING)
        LocalDateTime createdOn,

        String description,

        @JsonFormat(pattern = Constants.JSON_TIME_FORMAT, shape = JsonFormat.Shape.STRING)
        LocalDateTime eventDate,

        Long id,

        UserShortDto initiator,

        LocationDto location,

        boolean paid,

        Integer participantLimit,

        @JsonFormat(pattern = Constants.JSON_TIME_FORMAT, shape = JsonFormat.Shape.STRING)
        LocalDateTime publishedOn,

        boolean requestModeration,

        EventState state,

        String title,

        long views,

        long likesCount


) {

}
