package ru.practicum.user.service.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.user.NewUserRequest;
import ru.practicum.dto.user.UserDto;
import ru.practicum.exception.AlreadyExistsException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.user.mapper.UserMapper;
import ru.practicum.user.model.User;
import ru.practicum.user.repository.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public UserDto createUser(NewUserRequest newUserRequest) {
        log.info("Creating user with email: {}", newUserRequest.getEmail());

        checkEmailUniqueness(newUserRequest.getEmail());

        User user = userMapper.toEntity(newUserRequest);
        User savedUser = userRepository.save(user);

        log.info("User created with id: {}", savedUser.getId());
        return userMapper.toDto(savedUser);
    }

    @Override
    public List<UserDto> getUsers(List<Long> ids, int from, int size) {
        log.info("Getting users with ids: {}, from: {}, size: {}", ids, from, size);

        validatePaginationParams(from, size);

        Pageable pageable = PageRequest.of(from / size, size);
        List<User> users;

        if (ids == null || ids.isEmpty()) {
            users = userRepository.findAll(pageable).getContent();
        } else {
            users = userRepository.findAllById(ids);
        }

        return users.stream()
                .map(userMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        log.info("Deleting user with id: {}", userId);

        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User with id=" + userId + " was not found");
        }

        userRepository.deleteById(userId);
        log.info("User with id: {} deleted", userId);
    }

    @Override
    public User getUserById(Long userId) {
        log.debug("Getting user by id: {}", userId);
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));
    }

    @Override
    public boolean existsById(Long userId) {
        log.debug("Checking if user exists by id: {}", userId);
        return userRepository.existsById(userId);
    }

    private void checkEmailUniqueness(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new AlreadyExistsException("Email '" + email + "' already exists");
        }
    }

    private void validatePaginationParams(int from, int size) {
        if (from < 0) {
            throw new IllegalArgumentException("Parameter 'from' must be >= 0");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("Parameter 'size' must be > 0");
        }
    }
}