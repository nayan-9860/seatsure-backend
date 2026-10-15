package Seatsure.Backend.dto;

import Seatsure.Backend.enums.Status;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateShowRequestDto {

    @NotNull(message = "Event Id is required")
    private Long eventId;

    @NotNull(message = "Venue Id is required")
    private Long venueId;

    @NotNull(message = "Start time is required")
    private LocalDateTime startTime;

    @NotNull(message = "end time is required")
    private LocalDateTime endTime;

    @NotNull(message = "Show status is required")
    private Status status;

    @NotNull(message = "Regular price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Regular price must be greater than 0")
    private BigDecimal regularPrice;

    @NotNull(message = "Premium price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Premium price must be greater than 0")
    private BigDecimal premiumPrice;

    @NotNull(message = "VIP price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "VIP price must be greater than 0")
    private BigDecimal vipPrice;
}
