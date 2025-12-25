package ru.practicum.ewm.event.dto.params;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.ewm.request.dto.EventRequestStatusUpdateRequest;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventRequestUpdateParams {
    private Long userId;
    private Long eventId;
    private EventRequestStatusUpdateRequest updateRequest;
}
