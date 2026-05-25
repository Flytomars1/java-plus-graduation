package ru.practicum.recommendation.collector.service;

import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
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
            UserActionAvro avroMessage = UserActionAvro.newBuilder()
                    .setUserId(request.getUserId())
                    .setEventId(request.getEventId())
                    .setActionType(convertActionType(request.getActionType()))
                    .setTimestamp(timestamp)
                    .build();

            byte[] data = serializeAvro(avroMessage);

            kafkaTemplate.send("stats.user-actions.v1", request.getUserId(), data);

            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error processing user action", e);
            responseObserver.onError(e);
        }
    }

    private byte[] serializeAvro(UserActionAvro data) throws IOException {
        SpecificDatumWriter<UserActionAvro> writer = new SpecificDatumWriter<>(UserActionAvro.class);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(out, null);
        writer.write(data, encoder);
        encoder.flush();
        return out.toByteArray();
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