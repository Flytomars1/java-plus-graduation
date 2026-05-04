package ru.practicum.stats.server.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import ru.practicum.stats.server.model.EndpointHit;
import ru.practicum.stats.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.util.List;

public interface EndpointHitRepository extends CrudRepository<EndpointHit, Long> {

    @Query("""
        SELECT new ru.practicum.stats.dto.ViewStatsDto(e.app, e.uri, COUNT(e.id))
        FROM EndpointHit e
        WHERE e.timestamp BETWEEN :start AND :end
          AND (:urisNull = true OR e.uri IN :uris)
        GROUP BY e.app, e.uri
        ORDER BY COUNT(e.id) DESC
        """)
    List<ViewStatsDto> getStatsTotal(@Param("start") LocalDateTime start,
                                     @Param("end") LocalDateTime end,
                                     @Param("uris") List<String> uris,
                                     @Param("urisNull") boolean urisNull);

    @Query("""
        SELECT new ru.practicum.stats.dto.ViewStatsDto(e.app, e.uri, COUNT(DISTINCT e.ip))
        FROM EndpointHit e
        WHERE e.timestamp BETWEEN :start AND :end
          AND (:urisNull = true OR e.uri IN :uris)
        GROUP BY e.app, e.uri
        ORDER BY COUNT(DISTINCT e.ip) DESC
        """)
    List<ViewStatsDto> getStatsUnique(@Param("start") LocalDateTime start,
                                      @Param("end") LocalDateTime end,
                                      @Param("uris") List<String> uris,
                                      @Param("urisNull") boolean urisNull);
}