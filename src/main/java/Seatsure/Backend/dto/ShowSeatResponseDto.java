package Seatsure.Backend.dto;

import Seatsure.Backend.enums.SeatType;
import Seatsure.Backend.enums.ShowSeatStatus;
import lombok.*;

import java.math.BigDecimal;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ShowSeatResponseDto {

    private Long showSeatId;

    private Long seatId;

    private String seatNumber;

    private Integer rowNumber;

    private SeatType seatType;

    private ShowSeatStatus status;

    private BigDecimal price;
}
