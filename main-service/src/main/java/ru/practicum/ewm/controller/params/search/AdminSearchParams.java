package ru.practicum.ewm.controller.params.search;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import ru.practicum.ewm.entity.EventState;

import java.time.LocalDateTime;
import java.util.List;

@Data
@RequiredArgsConstructor
public class AdminSearchParams {

    private List<Long> users;
    private List<EventState> states;
    private List<Long> categories;
    private LocalDateTime rangeStart;
    private LocalDateTime rangeEnd;


}
