package ru.practicum.user.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.dto.user.NewUserRequest;
import ru.practicum.dto.user.UpdateUserRequest;
import ru.practicum.dto.user.UserDto;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.user.model.User;

@Component
public class UserMapper {

    public User toEntity(NewUserRequest dto) {
        return User.builder()
                .name(dto.getName())
                .email(dto.getEmail())
                .build();
    }

    public UserDto toDto(User entity) {
        return UserDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .email(entity.getEmail())
                .build();
    }

    public User updateFromDto(User user, UpdateUserRequest dto) {
        if (dto.getName() != null && !dto.getName().isBlank()) {
            user.setName(dto.getName());
        }
        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            user.setEmail(dto.getEmail());
        }
        return user;
    }

    public static UserShortDto toShortDto(User entity) {
        if (entity == null) return null;
        return new UserShortDto(entity.getId(), entity.getName());
    }
}