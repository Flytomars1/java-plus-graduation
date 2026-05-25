package ru.practicum.recommendation.collector.service;

import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.Schema;
import ru.practicum.ewm.stats.proto.Empty;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.proto.ActionTypeProto;
import ru.practicum.ewm.stats.proto.UserActionProto;
import ru.practicum.ewm.stats.proto.UserActionControllerGrpc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@GrpcService
public class UserActionControllerImpl extends UserActionControllerGrpc.UserActionControllerImplBase {

    private static final Logger log = LoggerFactory.getLogger(UserActionControllerImpl.class);

    @Autowired
    private KafkaTemplate<Long, byte[]> kafkaTemplate;

    @Override
    public void collectUserAction(UserActionProto request, StreamObserver<Empty> responseObserver) {
        long timestamp = request.getTimestamp();

        try {
            Schema schema = UserActionAvro.getClassSchema();
            GenericRecord avroRecord = new GenericData.Record(schema);

            avroRecord.put("userId", request.getUserId());
            avroRecord.put("eventId", request.getEventId());
            ActionTypeAvro actionTypeAvro = convertActionType(request.getActionType());
            avroRecord.put("actionType", actionTypeAvro);
            avroRecord.put("timestamp", timestamp);

            byte[] data = serializeAvro(avroRecord, schema);

            kafkaTemplate.send("stats.user-actions.v1", request.getUserId(), data);


            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error processing user action", e);
            responseObserver.onError(e);
        }
    }

    private byte[] serializeAvro(GenericRecord data, Schema schema) throws IOException {
        GenericDatumWriter<GenericRecord> writer = new GenericDatumWriter<>(schema);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(out, null);
        writer.write(data, encoder);
        encoder.flush();
        byte[] result = out.toByteArray();
        return result;
    }

    private ActionTypeAvro convertActionType(ActionTypeProto actionType) {
        switch (actionType) {
            case ACTION_VIEW: return ActionTypeAvro.VIEW;
            case ACTION_REGISTER: return ActionTypeAvro.REGISTER;
            case ACTION_LIKE: return ActionTypeAvro.LIKE;
            default:
                log.warn("Unknown action type: {}, defaulting to VIEW", actionType);
                return ActionTypeAvro.VIEW;
        }
    }
}