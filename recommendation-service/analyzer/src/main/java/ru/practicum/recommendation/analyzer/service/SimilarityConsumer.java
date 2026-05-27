package ru.practicum.recommendation.analyzer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.practicum.recommendation.analyzer.mapper.EventSimilarityMapper;
import ru.practicum.recommendation.analyzer.model.EventSimilarityEntity;
import ru.practicum.recommendation.analyzer.repository.EventSimilarityRepository;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvroSerializer;

@Slf4j
@Component
@RequiredArgsConstructor
public class SimilarityConsumer {

    private final EventSimilarityRepository eventSimilarityRepository;
    private final EventSimilarityMapper eventSimilarityMapper;

    @KafkaListener(topics = "stats.events-similarity.v1", groupId = "analyzer-group")
    public void consume(byte[] message) {
        try {
            EventSimilarityAvro similarityAvro = EventSimilarityAvroSerializer.deserialize(message);
            log.debug("Received similarity: eventA={}, eventB={}, score={}",
                    similarityAvro.getEventA(), similarityAvro.getEventB(), similarityAvro.getScore());

            EventSimilarityEntity entity = eventSimilarityRepository
                    .findByEventAAndEventB(similarityAvro.getEventA(), similarityAvro.getEventB())
                    .map(existing -> {
                        eventSimilarityMapper.updateEntity(existing, similarityAvro);
                        return existing;
                    })
                    .orElse(eventSimilarityMapper.toEntity(similarityAvro));

            eventSimilarityRepository.save(entity);
            log.debug("Saved similarity to database");

        } catch (Exception e) {
            log.error("Error processing similarity", e);
        }
    }
}