package ru.practicum.event.service.compilation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.client.UserClient;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.dto.compilation.CompilationDto;
import ru.practicum.dto.compilation.NewCompilationDto;
import ru.practicum.dto.compilation.UpdateCompilationRequest;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.exception.NotFoundException;
import ru.practicum.event.mapper.CompilationMapper;
import ru.practicum.event.mapper.EventMapper;
import ru.practicum.event.model.Category;
import ru.practicum.event.model.Compilation;
import ru.practicum.event.model.Event;
import ru.practicum.event.repository.CategoryRepository;
import ru.practicum.event.repository.CompilationRepository;
import ru.practicum.event.repository.EventRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompilationServiceImpl implements CompilationService {

    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final UserClient userClient;

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
        Map<Long, Category> categoriesMap = allCategories.stream()
                .collect(Collectors.toMap(Category::getId, category -> category));

        Map<Long, UserShortDto> usersMap = fetchUsers(userIds);

        List<EventShortDto> eventDtos = c.getEvents().stream()
                .map(event -> {
                    Category cat = categoriesMap.get(event.getCategoryId());
                    UserShortDto user = usersMap.get(event.getInitiatorId());

                    CategoryDto catDto = cat != null ?
                            new CategoryDto(cat.getId(), cat.getName()) : null;

                    return EventMapper.toShort(event, catDto, user, 0L, 0L, null);
                })
                .collect(Collectors.toList());

        return CompilationMapper.toDto(c, eventDtos);
    }

    private Map<Long, UserShortDto> fetchUsers(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            List<UserShortDto> users = userClient.getUsersShortByIds(new ArrayList<>(userIds));
            return users.stream()
                    .collect(Collectors.toMap(UserShortDto::getId, user -> user));
        } catch (Exception e) {
            log.warn("Could not fetch users batch: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }
}