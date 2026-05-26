package ru.practicum.recommendation.analyzer.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.practicum.recommendation.analyzer.mapper.UserActionMapper;
import ru.practicum.recommendation.analyzer.model.UserActionEntity;
import ru.practicum.recommendation.analyzer.repository.UserActionRepository;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;

import java.io.IOException;

@Slf4j
@Component
public class UserActionConsumer {

    @Autowired
    private UserActionRepository userActionRepository;

    @Autowired
    private UserActionMapper userActionMapper;

    @KafkaListener(topics = "stats.user-actions.v1", groupId = "analyzer-group")
    public void consume(byte[] message) {
        try {
            UserActionAvro userActionAvro = deserialize(message);
            log.debug("Received user action: userId={}, eventId={}, actionType={}",
                    userActionAvro.getUserId(), userActionAvro.getEventId(), userActionAvro.getActionType());

            UserActionEntity entity = userActionMapper.toEntity(userActionAvro);
            userActionRepository.save(entity);
            log.debug("Saved user action to database");

        } catch (Exception e) {
            log.error("Error processing user action", e);
        }
    }

    private UserActionAvro deserialize(byte[] data) throws IOException {
        SpecificDatumReader<UserActionAvro> reader = new SpecificDatumReader<>(UserActionAvro.getClassSchema());
        Decoder decoder = DecoderFactory.get().binaryDecoder(data, null);
        return reader.read(null, decoder);
    }
}