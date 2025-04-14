package ru.practicum.ewm.user.controller;

public record AdminUsersGetAllParams(
        Long[] ids,
        int from,
        int size
) {
}
