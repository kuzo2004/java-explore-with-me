package ru.practicum.ewm;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import ru.practicum.ewm.client.StatsClient;
import ru.practicum.ewm.dto.EndpointHitDto;
import ru.practicum.ewm.dto.ViewStats;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@SpringBootApplication
public class MainServiceApp {

    public static void main(String[] args) {
        SpringApplication.run(MainServiceApp.class, args);
    }

    /**
     * Этот бин выполнится после полной инициализации Spring-контекста.
     * Здесь можно безопасно вызывать StatsClient, чтобы убедиться,
     * что stats-server доступен.
     */
    @Bean
    public CommandLineRunner testStatsClient(StatsClient statsClient) {
        return args -> {
            System.out.println("=== Test StatsClient START v2 ===");

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            // ------------------------------ POST /hit ------------------------------
            EndpointHitDto hitDto = EndpointHitDto.builder()
                                                  .app("ewm-main-service")
                                                  .uri("/events")
                                                  .ip("123.45.67.89")
                                                  .timestamp(LocalDateTime.parse("2025-11-30 16:36:33", formatter))
                                                  .build();

            try {
                statsClient.saveHit(hitDto);
                System.out.println("Hit sent successfully");
            } catch (Exception e) {
                System.err.println("Failed to send hit: " + e.getMessage());
            }

            // ------------------------------ GET /stats ------------------------------
            DateTimeFormatter isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
            String start = LocalDateTime.of(2025, 11, 30, 16, 0).format(isoFormatter);
            String end = LocalDateTime.of(2025, 11, 30, 17, 0).format(isoFormatter);

            try {
                List<ViewStats> stats = statsClient.getStats(start, end, List.of("/events", "/films"), false);
                System.out.println("Received stats:");
                stats.forEach(System.out::println);
            } catch (Exception e) {
                System.err.println("Failed to fetch stats: " + e.getMessage());
            }

            System.out.println("=== Test StatsClient END ===");
        };
    }
}