package ru.practicum.controller;

public record AdminUsersGetAllParams(
        Long[] ids,
        int from,
        int size
) {
}
