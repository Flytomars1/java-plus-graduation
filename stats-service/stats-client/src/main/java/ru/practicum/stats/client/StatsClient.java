package ru.practicum.stats.client;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.MaxAttemptsRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.stats.dto.HitDto;
import ru.practicum.stats.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
public class StatsClient {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RestTemplate restTemplate;
    private final DiscoveryClient discoveryClient;
    private final RetryTemplate retryTemplate;
    private final String statsServiceId;

    public StatsClient(DiscoveryClient discoveryClient, RestTemplate restTemplate) {
        this.discoveryClient = discoveryClient;
        this.restTemplate = restTemplate;
        this.statsServiceId = "stats-server";

        this.retryTemplate = new RetryTemplate();

        FixedBackOffPolicy fixedBackOffPolicy = new FixedBackOffPolicy();
        fixedBackOffPolicy.setBackOffPeriod(3000L);
        retryTemplate.setBackOffPolicy(fixedBackOffPolicy);

        MaxAttemptsRetryPolicy retryPolicy = new MaxAttemptsRetryPolicy();
        retryPolicy.setMaxAttempts(3);
        retryTemplate.setRetryPolicy(retryPolicy);
    }

    private ServiceInstance getInstance() {
        List<ServiceInstance> instances = discoveryClient.getInstances(statsServiceId);
        if (instances == null || instances.isEmpty()) {
            throw new RuntimeException("Сервис статистики недоступен: " + statsServiceId);
        }
        return instances.get(0);
    }

    private String getBaseUrl() {
        ServiceInstance instance = retryTemplate.execute(context -> getInstance());
        return "http://" + instance.getHost() + ":" + instance.getPort();
    }

    public void hit(HitDto hitDto) {
        String baseUrl = getBaseUrl();
        restTemplate.postForEntity(baseUrl + "/hit", hitDto, Void.class);
    }

    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end,
                                       List<String> uris, boolean unique) {
        String baseUrl = getBaseUrl();

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl + "/stats")
                .queryParam("start", start.format(DATE_TIME_FORMATTER))
                .queryParam("end", end.format(DATE_TIME_FORMATTER))
                .queryParam("unique", unique);

        if (uris != null && !uris.isEmpty()) {
            uris.forEach(builder::queryParam);
        }

        String url = builder.toUriString();

        ViewStatsDto[] response = restTemplate.getForObject(url, ViewStatsDto[].class);
        return response == null ? Collections.emptyList() : Arrays.asList(response);
    }
}