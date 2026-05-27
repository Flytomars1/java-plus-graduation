package ru.practicum.stats.server.service;

import ru.practicum.stats.dto.HitDto;
import ru.practicum.stats.dto.ViewStatsDto;

import java.util.List;

public interface StatsService {

    void saveHit(HitDto dto);

    List<ViewStatsDto> getStats(String start, String end, List<String> uris, Boolean unique);
}