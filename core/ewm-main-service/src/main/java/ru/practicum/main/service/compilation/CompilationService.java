package ru.practicum.main.service.compilation;

import ru.practicum.main.dto.compilation.CompilationDto;
import ru.practicum.main.dto.compilation.NewCompilationDto;
import ru.practicum.main.dto.compilation.UpdateCompilationRequest;
import java.util.List;

public interface CompilationService {
    CompilationDto create(NewCompilationDto dto);
    CompilationDto update(Long compId, UpdateCompilationRequest dto);
    void delete(Long compId);
    CompilationDto getById(Long compId);
    List<CompilationDto> getAll(Boolean pinned, Integer from, Integer size);
}