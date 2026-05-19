package ru.practicum.user.service.user;

import org.springframework.stereotype.Component;
import ru.practicum.dto.user.UpdateUserRequest;
import ru.practicum.user.model.User;

@Component
public class UserUpdater {

    public User updateFromDto(User user, UpdateUserRequest dto) {
        if (dto.getName() != null && !dto.getName().isBlank()) {
            user.setName(dto.getName());
        }
        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            user.setEmail(dto.getEmail());
        }
        return user;
    }
}