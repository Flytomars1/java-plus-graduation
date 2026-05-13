package ru.practicum.client;

import org.springframework.cloud.openfeign.FeignClient;
import ru.practicum.api.RequestInternalApi;

@FeignClient(name = "request-service")
public interface RequestClient extends RequestInternalApi {
}