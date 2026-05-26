package ru.practicum.recommendation.aggregator.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Properties;

@Slf4j
@Component
public class KafkaProducerService {

    private final KafkaProducer<String, byte[]> kafkaProducer;
    private final String topic;

    public KafkaProducerService(
            @Value("${spring.kafka.bootstrap-servers:localhost:9092}") String bootstrapServers,
            @Value("${kafka.topics.events-similarity:stats.events-similarity.v1}") String topic) {

        this.topic = topic;

        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.ByteArraySerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);

        this.kafkaProducer = new KafkaProducer<>(props);
        log.info("KafkaProducer initialized for topic: {}", topic);
    }

    public void sendSimilarity(EventSimilarityAvro similarity) {
        try {
            byte[] data = serialize(similarity);
            ProducerRecord<String, byte[]> record = new ProducerRecord<>(
                    topic,
                    String.valueOf(Math.min(similarity.getEventA(), similarity.getEventB())),
                    data
            );

            kafkaProducer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    log.error("Failed to send similarity to Kafka", exception);
                } else {
                    log.debug("Sent similarity: eventA={}, eventB={}, score={}",
                            similarity.getEventA(), similarity.getEventB(), similarity.getScore());
                }
            });
        } catch (Exception e) {
            log.error("Error sending similarity", e);
        }
    }

    private byte[] serialize(EventSimilarityAvro data) throws IOException {
        SpecificDatumWriter<EventSimilarityAvro> writer = new SpecificDatumWriter<>(EventSimilarityAvro.getClassSchema());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(out, null);
        writer.write(data, encoder);
        encoder.flush();
        return out.toByteArray();
    }
}