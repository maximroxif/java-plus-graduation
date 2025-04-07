package ru.practicum.ewm.user.service;

import ru.practicum.ewm.user.controller.AdminUsersGetAllParams;
import ru.practicum.ewm.user.dto.UserCreateDto;
import ru.practicum.ewm.user.dto.UserDto;

import java.util.List;

public interface UserService {

    UserDto add(UserCreateDto userCreateDto);

    List<UserDto> getAll(AdminUsersGetAllParams adminUsersGetAllParams);

    void delete(long userId);

}
