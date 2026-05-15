package ru.practicum.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.practicum.dto.user.UserDto;
import ru.practicum.dto.user.UserShortDto;

import java.util.List;

public interface UserInternalApi {

    @GetMapping("/internal/users/{userId}")
    UserDto getUserById(@PathVariable("userId") Long userId);

    @GetMapping("/internal/users/{userId}/short")
    UserShortDto getUserShortById(@PathVariable("userId") Long userId);

    @GetMapping("/internal/users/exists/{userId}")
    Boolean userExists(@PathVariable("userId") Long userId);

    @PostMapping("/internal/users/batch/short")
    List<UserShortDto> getUsersShortByIds(@RequestBody List<Long> userIds);
}