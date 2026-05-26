package ru.practicum.stats.server.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.practicum.stats.dto.HitDto;
import ru.practicum.stats.dto.ViewStatsDto;
import ru.practicum.stats.server.model.EndpointHit;
import ru.practicum.stats.server.repository.EndpointHitRepository;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatsServiceImpl implements StatsService {

    private final EndpointHitRepository repository;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    @Transactional
    public void saveHit(HitDto dto) {
        log.debug("Saving hit: app={}, uri={}, ip={}, timestamp={}",
                dto.getApp(), dto.getUri(), dto.getIp(), dto.getTimestamp());

        EndpointHit hit = EndpointHit.builder()
                .app(dto.getApp())
                .uri(dto.getUri())
                .ip(dto.getIp())
                .timestamp(dto.getTimestamp())
                .build();
        repository.save(hit);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ViewStatsDto> getStats(String start, String end, List<String> uris, Boolean unique) {
        LocalDateTime startDate = parseDateTime(start);
        LocalDateTime endDate = parseDateTime(end);

        if (endDate.isBefore(startDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "end must be after start");
        }

        boolean urisNull = (uris == null || uris.isEmpty());

        log.debug("Getting stats: start={}, end={}, uris={}, unique={}", startDate, endDate, uris, unique);

        return unique
                ? repository.getStatsUnique(startDate, endDate, uris, urisNull)
                : repository.getStatsTotal(startDate, endDate, uris, urisNull);
    }

    private LocalDateTime parseDateTime(String dateStr) {
        try {
            String decoded = URLDecoder.decode(dateStr, StandardCharsets.UTF_8);
            return LocalDateTime.parse(decoded, FORMATTER);
        } catch (Exception e) {
            log.error("Failed to parse date: {}", dateStr, e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid date format: " + dateStr, e);
        }
    }
}