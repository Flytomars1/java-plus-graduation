package ru.practicum.client;

import org.springframework.cloud.openfeign.FeignClient;
import ru.practicum.api.RatingInternalApi;

@FeignClient(name = "rating-service")
public interface RatingClient extends RatingInternalApi {
}