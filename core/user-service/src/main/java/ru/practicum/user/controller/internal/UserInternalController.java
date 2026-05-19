package ru.practicum.user.controller.internal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.user.UserDto;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.user.mapper.UserMapper;
import ru.practicum.user.model.User;
import ru.practicum.user.service.user.UserService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
@Slf4j
public class UserInternalController {

    private final UserService userService;

    @GetMapping("/{userId}")
    public UserDto getUserById(@PathVariable Long userId) {
        log.debug("Internal API: get user by id {}", userId);
        User user = userService.getUserById(userId);
        return UserMapper.toDto(user);
    }

    @GetMapping("/{userId}/short")
    public UserShortDto getUserShortById(@PathVariable Long userId) {
        log.debug("Internal API: get user short by id {}", userId);
        User user = userService.getUserById(userId);
        return UserMapper.toShortDto(user);
    }

    @GetMapping("/exists/{userId}")
    public Boolean userExists(@PathVariable Long userId) {
        log.debug("Internal API: check if user {} exists", userId);
        return userService.existsById(userId);
    }

    @PostMapping("/batch/short")
    public List<UserShortDto> getUsersShortByIds(@RequestBody List<Long> userIds) {
        log.debug("Internal API: get users short by ids: {}", userIds);
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        return userService.getUsersByIds(userIds).stream()
                .map(UserMapper::toShortDto)
                .collect(Collectors.toList());
    }
}