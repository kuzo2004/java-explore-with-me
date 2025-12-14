package ru.practicum.ewm.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewCategoryDto {

    @NotBlank(message = "must not be blank")
    @Size(min = 1, max = 50, message = "length must be between 1 and 50")
    private String name;
}
