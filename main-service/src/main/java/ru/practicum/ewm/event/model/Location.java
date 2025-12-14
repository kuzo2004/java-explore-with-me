package ru.practicum.ewm.event.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Embeddable
public class Location {

    @Column(nullable = false, name = "lat")
    private Float lat;

    @Column(nullable = false, name = "lon")
    private Float lon;
}
