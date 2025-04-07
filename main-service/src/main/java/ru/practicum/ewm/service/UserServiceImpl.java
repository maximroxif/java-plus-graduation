package ru.practicum.ewm.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.controller.admin.AdminUsersGetAllParams;
import ru.practicum.ewm.dto.user.UserCreateDto;
import ru.practicum.ewm.dto.user.UserDto;
import ru.practicum.ewm.entity.User;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mapper.UserMapper;
import ru.practicum.ewm.repository.UserRepository;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public UserDto add(UserCreateDto userCreateDto) {
        User user = userMapper.userCreateDtoToUser(userCreateDto);
        return userMapper.userToUserDto(userRepository.save(user));
    }

    @Override
    @Transactional
    public List<UserDto> getAll(AdminUsersGetAllParams adminUsersGetAllParams) {
        List<User> userSearchList;
        if (adminUsersGetAllParams.ids() != null) {
            userSearchList = userRepository.findAllByIdIn(
                    Arrays.asList(adminUsersGetAllParams.ids()), PageRequest.of(adminUsersGetAllParams.from(), adminUsersGetAllParams.size()));
        } else {
            userSearchList =
                    userRepository.findAll(
                            PageRequest.of(
                                    adminUsersGetAllParams.from(), adminUsersGetAllParams.size())).stream().toList();
        }

        return userSearchList.stream()
                .map(userMapper::userToUserDto)
                .toList();
    }

    @Override
    @Transactional
    public void delete(long userId) {
        try {
            userRepository.findById(userId);
        } catch (EmptyResultDataAccessException e) {
            throw new NotFoundException("Required user with id " + userId + " was not found.");
        }
        userRepository.deleteById(userId);
    }
}
