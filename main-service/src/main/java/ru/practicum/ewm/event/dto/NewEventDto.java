package ru.practicum.ewm.event.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.ewm.event.model.Location;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewEventDto {

    @NotBlank(message = "must not be blank")
    @Size(min = 20, max = 2000)
    private String annotation;

    @NotBlank(message = "must not be blank")
    @Size(min = 20, max = 7000)
    private String description;

    @NotNull(message = "must not be null")
    private Long category;

    @NotNull(message = "must not be null")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime eventDate;

    @NotNull(message = "must not be null")
    private Location location;

    @Builder.Default
    private Boolean paid = false;

    @Min(value = 0, message = "participantLimit must be zero or positive")
    @Builder.Default
    private Integer participantLimit = 0;

    @Builder.Default
    private Boolean requestModeration = true;

    @NotBlank(message = "must not be blank")
    @Size(min = 3, max = 120)
    private String title;
}
