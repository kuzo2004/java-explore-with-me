package ru.practicum.ewm.event.dto.params;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.ewm.event.dto.UpdateEventUserRequest;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EventParams {

    private Long userId;

    private Long eventId;

    private UpdateEventUserRequest dto;
}