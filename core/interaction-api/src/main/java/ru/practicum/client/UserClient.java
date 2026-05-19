package ru.practicum.client;

import org.springframework.cloud.openfeign.FeignClient;
import ru.practicum.api.UserInternalApi;

@FeignClient(name = "user-service")
public interface UserClient extends UserInternalApi {
}