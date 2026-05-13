package ru.practicum.client;

import org.springframework.cloud.openfeign.FeignClient;
import ru.practicum.api.EventInternalApi;

@FeignClient(name = "event-service")
public interface EventClient extends EventInternalApi {
}