package ru.practicum.recommendation.analyzer.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.recommendation.analyzer.model.EventSimilarityEntity;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import java.time.Instant;

@Component
public class EventSimilarityMapper {

    public EventSimilarityEntity toEntity(EventSimilarityAvro avro) {
        if (avro == null) {
            return null;
        }

        return EventSimilarityEntity.builder()
                .eventA(avro.getEventA())
                .eventB(avro.getEventB())
                .score(avro.getScore())
                .timestamp(Instant.now())
                .build();
    }

    public void updateEntity(EventSimilarityEntity entity, EventSimilarityAvro avro) {
        if (entity == null || avro == null) {
            return;
        }

        entity.setScore(avro.getScore());
        entity.setTimestamp(Instant.now());
    }
}