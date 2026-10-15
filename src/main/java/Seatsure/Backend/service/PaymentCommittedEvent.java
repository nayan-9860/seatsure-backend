package Seatsure.Backend.service;

public record PaymentCommittedEvent(
        Long showId,
        Long showSeatId,
        Long userId
) {
}
