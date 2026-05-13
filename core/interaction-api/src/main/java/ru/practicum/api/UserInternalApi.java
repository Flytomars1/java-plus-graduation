package ru.practicum.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.practicum.dto.user.UserDto;
import ru.practicum.dto.user.UserShortDto;

public interface UserInternalApi {

    @GetMapping("/internal/users/{userId}")
    UserDto getUserById(@PathVariable("userId") Long userId);

    @GetMapping("/internal/users/{userId}/short")
    UserShortDto getUserShortById(@PathVariable("userId") Long userId);

    @GetMapping("/internal/users/exists/{userId}")
    Boolean userExists(@PathVariable("userId") Long userId);
}