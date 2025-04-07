package ru.practicum.ewm.request.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import ru.practicum.ewm.request.dto.ParticipationRequestDto;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.request.model.Request;
import ru.practicum.ewm.request.model.RequestStatus;
import ru.practicum.ewm.user.model.User;

import java.time.LocalDateTime;

@Mapper(componentModel = "spring")
public abstract class RequestMapper {

    @Mapping(target = "event", expression = "java(request.getEvent().getId())")
    @Mapping(target = "requester", expression = "java(request.getRequester().getId())")
    public abstract ParticipationRequestDto toParticipationRequestDto(Request request);

    @Mapping(target = "created", expression = "java(getCurrentLocalDatetime())")
    @Mapping(target = "status", expression = "java(setRequestStatus(event))")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "requester", source = "user")
    @Mapping(target = "event", source = "event")
    public abstract Request toRequest(User user, Event event);

    @Named("getCurrentLocalDatetime")
    LocalDateTime getCurrentLocalDatetime() {
        return LocalDateTime.now().withNano(0);
    }

    @Named("getPendingEventState")
    RequestStatus setRequestStatus(Event event) {
        if (!event.isRequestModeration() || event.getParticipantLimit() == 0) {
            return RequestStatus.CONFIRMED;
        }
        return RequestStatus.PENDING;
    }



}
