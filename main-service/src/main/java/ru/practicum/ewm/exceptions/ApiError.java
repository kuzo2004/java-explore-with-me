package ru.practicum.ewm.exceptions;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiError {
    private String status;           // e.g. "BAD_REQUEST"
    private String reason;           // short reason
    private String message;          // human message
    private LocalDateTime timestamp;
    private List<String> errors;     // optional stacktraces or details
}
