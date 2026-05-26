package ru.practicum.recommendation.analyzer.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.practicum.recommendation.analyzer.mapper.EventSimilarityMapper;
import ru.practicum.recommendation.analyzer.model.EventSimilarityEntity;
import ru.practicum.recommendation.analyzer.repository.EventSimilarityRepository;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

import java.io.IOException;

@Slf4j
@Component
public class SimilarityConsumer {

    @Autowired
    private EventSimilarityRepository eventSimilarityRepository;

    @Autowired
    private EventSimilarityMapper eventSimilarityMapper;

    @KafkaListener(topics = "stats.events-similarity.v1", groupId = "analyzer-group")
    public void consume(byte[] message) {
        try {
            EventSimilarityAvro similarityAvro = deserialize(message);
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

    private EventSimilarityAvro deserialize(byte[] data) throws IOException {
        SpecificDatumReader<EventSimilarityAvro> reader = new SpecificDatumReader<>(EventSimilarityAvro.getClassSchema());
        Decoder decoder = DecoderFactory.get().binaryDecoder(data, null);
        return reader.read(null, decoder);
    }
}