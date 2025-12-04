package ru.practicum.ewm.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EndpointHitDto {

    private Long id;

    @NotBlank(message = "app must not be blank")
    private String app;

    @NotBlank(message = "uri must not be blank")
    private String uri;

    @NotBlank(message = "ip must not be blank")
    private String ip;

    @NotNull(message = "timestamp must not be null")
    @JsonFormat(shape = JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
}
