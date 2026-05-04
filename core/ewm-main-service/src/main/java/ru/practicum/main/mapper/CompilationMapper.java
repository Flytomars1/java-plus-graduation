package ru.practicum.main.mapper;

import ru.practicum.main.dto.compilation.CompilationDto;
import ru.practicum.main.dto.compilation.NewCompilationDto;
import ru.practicum.main.dto.event.EventShortDto;
import ru.practicum.main.model.Compilation;
import ru.practicum.main.model.Event;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CompilationMapper {

    public static Compilation toEntity(NewCompilationDto dto, Set<Event> events) {
        return Compilation.builder()
                .pinned(dto.getPinned() != null ? dto.getPinned() : false)
                .title(dto.getTitle())
                .events(events)
                .build();
    }

    public static CompilationDto toDto(Compilation compilation, List<EventShortDto> eventDtos) {
        return CompilationDto.builder()
                .id(compilation.getId())
                .pinned(compilation.getPinned())
                .title(compilation.getTitle())
                .events(eventDtos)
                .build();
    }
}