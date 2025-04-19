package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.controller.AdminUsersGetAllParams;
import ru.practicum.dto.user.UserCreateDto;
import ru.practicum.dto.user.UserDto;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.UserMapper;
import ru.practicum.model.User;
import ru.practicum.repository.UserRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public UserDto add(UserCreateDto userCreateDto) {
        log.info("Adding new user: {}", userCreateDto);
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
        User user = findUserById(userId);
        userRepository.delete(user);
        log.info("User with id={} successfully deleted", userId);
    }

    @Override
    @Transactional(readOnly = true)
    public void checkExistence(long userId) {
        log.debug("Checking existence of user with id: {}", userId);
        findUserById(userId);
        log.debug("User with id={} exists", userId);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getById(long userId) {
        log.info("Fetching user with id: {}", userId);
        User user = findUserById(userId);
        log.debug("User with id={} found", userId);
        return userMapper.userToUserDto(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, UserDto> getByIds(List<Long> userIds) {
        log.info("Fetching users with ids: {}", userIds);
        if (userIds == null || userIds.isEmpty()) {
            log.warn("Empty or null userIds provided, returning empty map");
            return Map.of();
        }

        List<User> users = userRepository.findAllById(userIds);
        log.info("Found {} users for provided ids", users.size());

        Map<Long, UserDto> usersMap = users.stream()
                .collect(Collectors.toMap(
                        User::getId,
                        userMapper::userToUserDto
                ));

        return usersMap;
    }

    private User findUserById(long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User with id={} not found", userId);
                    return new NotFoundException("User with id=" + userId + " not found");
                });
    }
}