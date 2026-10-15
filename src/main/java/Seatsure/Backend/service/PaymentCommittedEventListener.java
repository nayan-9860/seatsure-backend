
package Seatsure.Backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class PaymentCommittedEventListener {

    private final SeatHoldCleanupService seatHoldCleanupService;

    // Handle successful payments only after the database transaction commits.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentCommitted(PaymentCommittedEvent event) {

        seatHoldCleanupService.releaseSeatHold(
                event.showId(),
                event.showSeatId(),
                event.userId()
        );
    }

    // Handle expiry, failure, and cancellation cleanup after the
    // corresponding database transaction commits successfully.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleSeatHoldRelease(SeatHoldReleaseEvent event) {

        seatHoldCleanupService.releaseSeatHold(
                event.showId(),
                event.showSeatId(),
                event.userId()
        );
    }
}