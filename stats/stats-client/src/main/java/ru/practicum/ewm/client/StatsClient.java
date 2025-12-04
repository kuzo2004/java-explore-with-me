package ru.practicum.ewm.client;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.ewm.dto.EndpointHitDto;
import ru.practicum.ewm.dto.ViewStats;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Это клиент для stats-service, который знает, как сформировать HTTP-запрос
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StatsClient {

    // Подхватываем URL из Spring Environment
    @Value("${client.url:http://localhost:9090}")
    private String statsBaseUrl;


    private RestClient restClient;

    // Инициализация RestClient после того, как Spring подставит statsBaseUrl
    @PostConstruct
    public void init() {
        this.restClient = RestClient.builder()
                                    .baseUrl(statsBaseUrl)
                                    .build();
        log.info("StatsClient initialized with URL: {}", statsBaseUrl);
    }

    /**
     * Нужен для корректной десериализации JSON -> List<ViewStats>.
     * Java стирает generics во время выполнения (type erasure), поэтому RestClient
     * не может узнать тип элементов списка.
     * ParameterizedTypeReference сохраняет полную информацию о типе List<ViewStats>.
     */
    private static final ParameterizedTypeReference<List<ViewStats>> LIST_OF_VIEW_STATS =
            new ParameterizedTypeReference<>() {
            };

    // --------------------------
    // POST /hit
    // --------------------------
    public void saveHit(EndpointHitDto dto) {
        try {
            restClient
                    .post()
                    .uri("/hit")
                    .body(dto)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.warn("Stats service unavailable, hit not sent: {}", e.getMessage());
            // ошибка «поглощена», основной сервис продолжает работать
        }
    }

    // --------------------------
    // GET /stats
    // --------------------------
    public List<ViewStats> getStats(
            String start,
            String end,
            List<String> uris,
            boolean unique
    ) {
        try {
            Optional<List<String>> optionalUris =
                    (uris == null || uris.isEmpty())
                            ? Optional.empty()
                            : Optional.of(uris);

            String url = UriComponentsBuilder.fromPath("/stats")
                                             .queryParam("start", start)
                                             .queryParam("end", end)
                                             .queryParam("unique", unique)
                                             .queryParamIfPresent("uris", optionalUris)
                                             .toUriString();

            return restClient
                    .get()
                    .uri(url)
                    .retrieve()
                    .body(LIST_OF_VIEW_STATS);

        } catch (Exception e) {
            log.warn("Stats service unavailable, returning empty stats: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
