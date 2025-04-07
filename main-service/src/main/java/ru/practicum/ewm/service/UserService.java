package ru.practicum.ewm.service;

import ru.practicum.ewm.controller.admin.AdminUsersGetAllParams;
import ru.practicum.ewm.dto.user.UserCreateDto;
import ru.practicum.ewm.dto.user.UserDto;

import java.util.List;

public interface UserService {

    UserDto add(UserCreateDto userCreateDto);

    List<UserDto> getAll(AdminUsersGetAllParams adminUsersGetAllParams);

    void delete(long userId);

}
