package Seatsure.Backend.config;

import Seatsure.Backend.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingExpiryScheduler {

    private final BookingService bookingService;

    @Scheduled(fixedDelay = 60000)
    public void expireBookings() {
        int expiredCount = bookingService.expirePendingBookings();
        if (expiredCount > 0) {
            log.info(
                    "Expired {} pending booking(s)",
                    expiredCount );
        }
    }
}
