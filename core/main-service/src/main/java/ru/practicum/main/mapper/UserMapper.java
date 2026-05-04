package ru.practicum.main.mapper;

import ru.practicum.main.dto.user.NewUserRequest;
import ru.practicum.main.dto.user.UpdateUserRequest;
import ru.practicum.main.dto.user.UserDto;
import ru.practicum.main.model.User;
import lombok.experimental.UtilityClass;

@UtilityClass
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
}