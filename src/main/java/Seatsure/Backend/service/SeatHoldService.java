package Seatsure.Backend.service;

import Seatsure.Backend.entity.ShowSeat;
import Seatsure.Backend.entity.User;
import Seatsure.Backend.enums.ShowSeatStatus;
import Seatsure.Backend.exception.*;
import Seatsure.Backend.repository.ShowSeatRepository;
import Seatsure.Backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;

@Service
@RequiredArgsConstructor
public class SeatHoldService {

    private final ShowSeatRepository showSeatRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final UserRepository userRepository;

    private static final Duration HOLD_DURATION =
            Duration.ofMinutes(5);

    public void holdSeat(
            Long showId,
            Long showSeatId,
            String userEmail) {

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));

        ShowSeat showSeat = showSeatRepository
                .findById(showSeatId)
                .orElseThrow(() ->
                        new ShowSeatNotFoundException("Show seat not found"));

        if (!showSeat.getShow().getId().equals(showId)) {
            throw new ShowSeatNotFoundException(
                    "Selected seat does not belong to this show");
        }

        if (showSeat.getStatus() != ShowSeatStatus.AVAILABLE) {
            throw new SeatNotAvailableException(
                    "This show seat is not available");
        }

        Long userId = user.getId();

        String key = buildKey(showId, showSeatId);

        Boolean created = redisTemplate.opsForValue().setIfAbsent(
                key,
                userId.toString(),
                HOLD_DURATION
        );

        if (!Boolean.TRUE.equals(created)) {
            throw new SeatAlreadyHeldException(
                    "Seat is already held by another user"
            );
        }
    }

    private String buildKey(Long showId, Long showSeatId) {
        return "seat-hold:show:"
                + showId
                + ":seat:"
                + showSeatId;
    }

    private static final DefaultRedisScript<Long> RELEASE_HOLD_SCRIPT =
            new DefaultRedisScript<>(
                    """
                    if redis.call('GET', KEYS[1]) == ARGV[1] then
                        return redis.call('DEL', KEYS[1])
                    else
                        return 0
                    end
                    """,
                    Long.class
            );


    public void releaseSeat(
            Long showId,
            Long showSeatId,
            String userEmail) {

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() ->
                        new UserNotFoundException("User not found"));

        String key = buildKey(showId, showSeatId);

        Long deleted = redisTemplate.execute(
                RELEASE_HOLD_SCRIPT,
                Collections.singletonList(key),
                user.getId().toString()
        );

        if (!Long.valueOf(1).equals(deleted)) {
            throw new SeatHoldReleaseException(
                    "Seat hold could not be released. "
                            + "It may have expired or may belong to another user."
            );
        }
    }

    public boolean releaseSeatIfOwned(
            Long showId,
            Long showSeatId,
            Long userId) {

        String key = buildKey(showId, showSeatId);

        Long deleted = redisTemplate.execute(
                RELEASE_HOLD_SCRIPT,
                Collections.singletonList(key),
                userId.toString()
        );

        return Long.valueOf(1).equals(deleted);
    }

    public boolean isSeatHeldByUser(
            Long showId,
            Long showSeatId,
            String userEmail) {

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() ->
                        new UserNotFoundException("User not found"));

        String key = buildKey(showId, showSeatId);

        String holdOwner = redisTemplate.opsForValue().get(key);

        return holdOwner != null
                && holdOwner.equals(user.getId().toString());
    }


}
