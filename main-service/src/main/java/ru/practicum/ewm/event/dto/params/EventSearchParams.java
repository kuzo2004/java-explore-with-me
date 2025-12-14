package ru.practicum.ewm.event.dto.params;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventSearchParams {
    private String text;                 // Поиск по описанию/аннотации
    private List<Long> categories;       // Список id категорий
    private Boolean paid;                // true / false
    private LocalDateTime rangeStart;
    private LocalDateTime rangeEnd;
    private Boolean onlyAvailable;       // true = только с доступным лимитом
    private String sort;                 // EVENT_DATE или VIEWS
}