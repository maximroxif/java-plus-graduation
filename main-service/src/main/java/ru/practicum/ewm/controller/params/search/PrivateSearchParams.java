package ru.practicum.ewm.controller.params.search;

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
