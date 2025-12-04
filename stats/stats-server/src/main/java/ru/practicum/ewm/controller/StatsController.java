package ru.practicum.ewm.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.ewm.dto.EndpointHitDto;
import ru.practicum.ewm.dto.ViewStats;
import ru.practicum.ewm.exceptions.BadRequestException;
import ru.practicum.ewm.mapper.EndpointHitMapper;
import ru.practicum.ewm.model.EndpointHit;
import ru.practicum.ewm.service.StatsService;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;
    private final EndpointHitMapper hitMapper;

    private static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /**
     * POST /hit — сохраняет информацию о запросе, тело ответа пустое
     */
    @PostMapping("/hit")
    @ResponseStatus(HttpStatus.CREATED)
    public void saveHit(@RequestBody @Valid EndpointHitDto hitDto) {
        log.info("POST /hit request received: {}", hitDto);

        EndpointHit hit = hitMapper.toEntity(hitDto);
        statsService.saveHit(hit);

        log.info("POST /hit successfully saved: id={}", hit.getId());
    }

    /**
     * GET /stats — возвращает статистику посещений
     */
    @GetMapping("/stats")
    @ResponseStatus(HttpStatus.OK)
    public List<ViewStats> getStats(
            @RequestParam("start") @DateTimeFormat(pattern = DATE_PATTERN) LocalDateTime start,
            @RequestParam("end") @DateTimeFormat(pattern = DATE_PATTERN) LocalDateTime end,
            @RequestParam(value = "uris", required = false) List<String> uris,
            @RequestParam(value = "unique", defaultValue = "false") boolean unique
    ) {
        log.info("GET /stats request received: start={}, end={}, uris={}, unique={}", start, end, uris, unique);

        if (start.isAfter(end)) {
            throw new BadRequestException("start must be before or equal to end");
        }

        List<ViewStats> stats = statsService.getStats(start, end, uris, unique);
        log.info("GET /stats returned {} records", stats.size());

        return stats;
    }
}
