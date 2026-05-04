package ru.practicum.main.service.compilation;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main.dto.compilation.*;
import ru.practicum.main.dto.event.EventShortDto;
import ru.practicum.main.dto.category.CategoryDto;
import ru.practicum.main.dto.user.UserShortDto;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.mapper.CompilationMapper;
import ru.practicum.main.mapper.EventMapper;
import ru.practicum.main.model.Compilation;
import ru.practicum.main.model.Event;
import ru.practicum.main.model.Category;
import ru.practicum.main.model.User;
import ru.practicum.main.repository.CompilationRepository;
import ru.practicum.main.repository.EventRepository;
import ru.practicum.main.repository.CategoryRepository;
import ru.practicum.main.repository.UserRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompilationServiceImpl implements CompilationService {

    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public CompilationDto create(NewCompilationDto dto) {
        Set<Event> events = new HashSet<>();
        if (dto.getEvents() != null && !dto.getEvents().isEmpty()) {
            events = new HashSet<>(eventRepository.findAllById(dto.getEvents()));
        }

        Compilation c = CompilationMapper.toEntity(dto, events);
        return toDtoWithEvents(compilationRepository.save(c));
    }

    @Override
    @Transactional
    public CompilationDto update(Long compId, UpdateCompilationRequest dto) {
        Compilation c = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Compilation not found"));

        if (dto.getEvents() != null) {
            c.setEvents(new HashSet<>(eventRepository.findAllById(dto.getEvents())));
        }
        if (dto.getPinned() != null) {
            c.setPinned(dto.getPinned());
        }
        if (dto.getTitle() != null && !dto.getTitle().isBlank()) {
            c.setTitle(dto.getTitle());
        }

        return toDtoWithEvents(compilationRepository.save(c));
    }

    @Override
    @Transactional
    public void delete(Long compId) {
        if (!compilationRepository.existsById(compId)) {
            throw new NotFoundException("Compilation not found");
        }
        compilationRepository.deleteById(compId);
    }

    @Override
    public CompilationDto getById(Long compId) {
        Compilation c = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Compilation not found"));
        return toDtoWithEvents(c);
    }

    @Override
    public List<CompilationDto> getAll(Boolean pinned, Integer from, Integer size) {
        PageRequest page = PageRequest.of(from / size, size);
        Page<Compilation> compilations;

        if (pinned != null) {
            compilations = compilationRepository.findAllByPinned(pinned, page);
        } else {
            compilations = compilationRepository.findAll(page);
        }

        return compilations.getContent().stream()
                .map(this::toDtoWithEvents)
                .collect(Collectors.toList());
    }

    private CompilationDto toDtoWithEvents(Compilation c) {
        if (c.getEvents() == null || c.getEvents().isEmpty()) {
            return CompilationMapper.toDto(c, Collections.emptyList());
        }

        Set<Long> categoryIds = new HashSet<>();
        Set<Long> userIds = new HashSet<>();

        for (Event event : c.getEvents()) {
            categoryIds.add(event.getCategoryId());
            userIds.add(event.getInitiatorId());
        }

        List<Category> allCategories = categoryRepository.findAllById(categoryIds);
        List<User> allUsers = userRepository.findAllById(userIds);

        Map<Long, Category> categoriesMap = allCategories.stream()
                .collect(Collectors.toMap(Category::getId, category -> category));

        Map<Long, User> usersMap = allUsers.stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        List<EventShortDto> eventDtos = c.getEvents().stream()
                .map(event -> {
                    Category cat = categoriesMap.get(event.getCategoryId());
                    User user = usersMap.get(event.getInitiatorId());

                    CategoryDto catDto = cat != null ?
                            new CategoryDto(cat.getId(), cat.getName()) : null;
                    UserShortDto userDto = user != null ?
                            new UserShortDto(user.getId(), user.getName()) : null;

                    return EventMapper.toShort(event, catDto, userDto, 0L, 0L);
                })
                .collect(Collectors.toList());

        return CompilationMapper.toDto(c, eventDtos);
    }
}