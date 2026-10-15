package Seatsure.Backend.dto;

import Seatsure.Backend.enums.SeatType;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateSeatRequestDto {

    @NotNull(message = "Venue ID is required")
    private Long venueId;

    @NotBlank(message = "Seat number is required")
    private String seatNumber;

    @NotNull(message = "Row number is required")
    @Min(value = 1, message = "Row number must be at least 1")
    private Integer rowNumber;

    @NotNull(message = "Seat type is required")
    private SeatType seatType;
}
