package ru.practicum.controller;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class EventSearchParams {

    AdminSearchParams adminSearchParams;
    PrivateSearchParams privateSearchParams;
    PublicSearchParams publicSearchParams;

    Integer from;
    Integer size;

}
