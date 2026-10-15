package Seatsure.Backend.dto;

import Seatsure.Backend.enums.SeatType;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingItemResponseDto {

    private Long bookingItemId;

    private Long showSeatId;

    private String seatNumber;

    private Integer rowNumber;

    private SeatType seatType;

    private BigDecimal price;
}
