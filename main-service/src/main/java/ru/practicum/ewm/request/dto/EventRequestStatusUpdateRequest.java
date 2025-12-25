package ru.practicum.ewm.request.dto;


import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventRequestStatusUpdateRequest {

    @NotEmpty(message = "RequestIds must not be empty")
    private List<Long> requestIds;

    @NotNull(message = "Status must not be null")
    private String status; // CONFIRMED или REJECTED
}
