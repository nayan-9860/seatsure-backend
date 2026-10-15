package Seatsure.Backend.service;

public record SeatHoldReleaseEvent(
        Long showId,
        Long showSeatId,
        Long userId
)  {
}
