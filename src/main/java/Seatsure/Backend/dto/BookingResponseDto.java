package Seatsure.Backend.dto;


import Seatsure.Backend.enums.BookingStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BookingResponseDto {

    private Long bookingId;

    private Long showId;

    private BookingStatus status;

    private BigDecimal totalAmount;

    private LocalDateTime expiresAt;

    private List<BookingItemResponseDto> bookingItems;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
