package Seatsure.Backend.dto;

import Seatsure.Backend.enums.SeatType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SeatResponseDto {


    private Long id;

    private Long venueId;

    private String seatNumber;

    private Integer rowNumber;

    private SeatType seatType;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
