package ru.practicum.recommendation.aggregator.service;

import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;

import java.io.IOException;

@Component
public class UserActionConsumer {

    private static final Logger log = LoggerFactory.getLogger(UserActionConsumer.class);

    @Autowired
    private SimilarityCalculator similarityCalculator;

    @KafkaListener(topics = "stats.user-actions.v1", groupId = "aggregator-group")
    public void consume(byte[] message) {
        try {
            UserActionAvro userAction = deserialize(message);
            log.debug("Received user action: userId={}, eventId={}, actionType={}",
                    userAction.getUserId(), userAction.getEventId(), userAction.getActionType());

            similarityCalculator.processUserAction(userAction);

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