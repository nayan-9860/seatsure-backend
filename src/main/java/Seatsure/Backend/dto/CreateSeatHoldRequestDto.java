package Seatsure.Backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateSeatHoldRequestDto {

    @NotNull(message = "Show ID is required")
    private Long showId;

    @NotNull(message = "Show seat ID is required")
    private Long showSeatId;

}
