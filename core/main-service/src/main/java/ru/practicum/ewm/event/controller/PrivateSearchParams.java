package ru.practicum.ewm.event.controller;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class PrivateSearchParams {

    private long initiatorId;

    public PrivateSearchParams(long initiatorId) {
        this.initiatorId = initiatorId;
    }
}
