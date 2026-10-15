package Seatsure.Backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SeatHoldCleanupService {

    private final SeatHoldService seatHoldService;

    @Retryable(
            includes = {
                    RedisConnectionFailureException.class,
                    RedisSystemException.class
            },
            maxRetries = 3,
            delay = 200,
            multiplier = 2,
            maxDelay = 1000
    )
    public void releaseSeatHold(Long showId, Long showSeatId, Long userId) {
        seatHoldService.releaseSeatIfOwned(showId, showSeatId, userId);
    }
}
