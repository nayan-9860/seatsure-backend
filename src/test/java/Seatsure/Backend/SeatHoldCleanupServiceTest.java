package Seatsure.Backend;

import Seatsure.Backend.service.SeatHoldCleanupService;
import Seatsure.Backend.service.SeatHoldService;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.resilience.annotation.EnableResilientMethods;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@SpringJUnitConfig(SeatHoldCleanupServiceTest.TestConfig.class)
class SeatHoldCleanupServiceTest {

    @Configuration
    @EnableResilientMethods
    static class TestConfig {

        @Bean
        SeatHoldService seatHoldService() {
            return mock(SeatHoldService.class);
        }

        @Bean
        SeatHoldCleanupService seatHoldCleanupService(
                SeatHoldService seatHoldService) {
            return new SeatHoldCleanupService(seatHoldService);
        }
    }

    @org.springframework.beans.factory.annotation.Autowired
    SeatHoldService seatHoldService;

    @org.springframework.beans.factory.annotation.Autowired
    SeatHoldCleanupService cleanupService;

    @Test
    void retriesWhenRedisConnectionFailsThenSucceeds() {
        when(seatHoldService.releaseSeatIfOwned(1L, 2L, 3L))
                .thenThrow(new RedisConnectionFailureException("Temporary Redis failure"))
                .thenReturn(true);

        assertDoesNotThrow(
                () -> cleanupService.releaseSeatHold(1L, 2L, 3L)
        );

        verify(seatHoldService, times(2))
                .releaseSeatIfOwned(1L, 2L, 3L);
    }


    @Test
    void stopsAfterConfiguredRetriesWhenRedisKeepsFailing() {
        org.mockito.Mockito.reset(seatHoldService);

        RedisConnectionFailureException failure =
                new RedisConnectionFailureException("Redis unavailable");

        when(seatHoldService.releaseSeatIfOwned(1L, 2L, 3L))
                .thenThrow(failure, failure, failure, failure);

        org.junit.jupiter.api.Assertions.assertThrows(
                RedisConnectionFailureException.class,
                () -> cleanupService.releaseSeatHold(1L, 2L, 3L)
        );

        verify(seatHoldService, times(4))
                .releaseSeatIfOwned(1L, 2L, 3L);
    }
}