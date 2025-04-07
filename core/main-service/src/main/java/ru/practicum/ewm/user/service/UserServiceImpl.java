package ru.practicum.ewm.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.user.controller.AdminUsersGetAllParams;
import ru.practicum.ewm.user.dto.UserCreateDto;
import ru.practicum.ewm.user.dto.UserDto;
import ru.practicum.ewm.user.mapper.UserMapper;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.repository.UserRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public UserDto add(UserCreateDto userCreateDto) {
        log.info("Adding new user {}", userCreateDto);
        User user = userMapper.userCreateDtoToUser(userCreateDto);
        User savedUser = userRepository.save(user);
        log.info("User successfully added with id: {}", savedUser.getId());
        return userMapper.userToUserDto(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDto> getAll(AdminUsersGetAllParams params) {
        log.info("Fetching users with params: from={}, size={}, ids={}",
                params.from(), params.size(), params.ids());
        PageRequest pageRequest = PageRequest.of(params.from(), params.size());
        List<User> users = (params.ids() != null && params.ids().length > 0)
                ? userRepository.findAllByIdIn(List.of(params.ids()), pageRequest)
                : userRepository.findAll(pageRequest).getContent();

        log.info("Found {} users", users.size());
        return users.stream()
                .map(userMapper::userToUserDto)
                .toList();
    }

    @Override
    @Transactional
    public void delete(long userId) {
        log.info("Attempting to delete user with id: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User with id={} not found", userId);
                    return new NotFoundException("User with id=" + userId + " not found");
                });
        userRepository.delete(user);
        log.info("User with id={} successfully deleted", userId);
    }
}