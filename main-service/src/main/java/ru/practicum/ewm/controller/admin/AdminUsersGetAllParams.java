package ru.practicum.ewm.controller.admin;

public record AdminUsersGetAllParams(
        Long[] ids,
        int from,
        int size
) {
}
