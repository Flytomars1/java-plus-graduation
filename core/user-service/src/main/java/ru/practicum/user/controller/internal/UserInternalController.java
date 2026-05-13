package ru.practicum.user.controller.internal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.user.UserDto;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.user.mapper.UserMapper;
import ru.practicum.user.model.User;
import ru.practicum.user.service.user.UserService;

@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
@Slf4j
public class UserInternalController {

    private final UserService userService;
    private final UserMapper userMapper;

    @GetMapping("/{userId}")
    public UserDto getUserById(@PathVariable Long userId) {
        log.debug("Internal API: get user by id {}", userId);
        User user = userService.getUserById(userId);
        return userMapper.toDto(user);
    }

    @GetMapping("/{userId}/short")
    public UserShortDto getUserShortById(@PathVariable Long userId) {
        log.debug("Internal API: get user short by id {}", userId);
        User user = userService.getUserById(userId);
        return new UserShortDto(user.getId(), user.getName());
    }

    @GetMapping("/exists/{userId}")
    public Boolean userExists(@PathVariable Long userId) {
        log.debug("Internal API: check if user {} exists", userId);
        return userService.existsById(userId);
    }
}