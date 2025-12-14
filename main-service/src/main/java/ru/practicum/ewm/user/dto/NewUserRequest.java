package ru.practicum.ewm.user.dto;


import jakarta.validation.constraints.Email;
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
public class NewUserRequest {

    @NotBlank(message = "must not be blank")
    @Email(message = "must be a valid email")
    @Size(min = 6, max = 254, message = "length must be between 6 and 254")
    private String email;

    @NotBlank(message = "must not be blank")
    @Size(min = 2, max = 250, message = "length must be between 2 and 250")
    private String name;
}
