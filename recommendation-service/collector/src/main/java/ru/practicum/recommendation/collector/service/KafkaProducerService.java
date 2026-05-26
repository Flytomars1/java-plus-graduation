package ru.practicum.recommendation.collector.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.LongSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Properties;

@Slf4j
@Component
public class KafkaProducerService {

    private final KafkaProducer<Long, byte[]> kafkaProducer;
    private final String topic;

    public KafkaProducerService(
            @Value("${spring.kafka.bootstrap-servers:localhost:9092}") String bootstrapServers,
            @Value("${kafka.topics.user-actions:stats.user-actions.v1}") String topic) {

        this.topic = topic;

        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.ByteArraySerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);

        this.kafkaProducer = new KafkaProducer<>(props);
        log.info("KafkaProducer initialized for topic: {}", topic);
    }

    public void sendUserAction(UserActionAvro userAction, Long userId) {
        try {
            byte[] data = serialize(userAction);
            ProducerRecord<Long, byte[]> record = new ProducerRecord<>(topic, userId, data);

            kafkaProducer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    log.error("Failed to send user action to Kafka", exception);
                } else {
                    log.debug("Sent user action: userId={}, eventId={}, actionType={}, offset={}",
                            userAction.getUserId(), userAction.getEventId(),
                            userAction.getActionType(), metadata.offset());
                }
            });
        } catch (Exception e) {
            log.error("Error sending user action", e);
        }
    }

    private byte[] serialize(UserActionAvro data) throws IOException {
        SpecificDatumWriter<UserActionAvro> writer = new SpecificDatumWriter<>(UserActionAvro.getClassSchema());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(out, null);
        writer.write(data, encoder);
        encoder.flush();
        return out.toByteArray();
    }
}