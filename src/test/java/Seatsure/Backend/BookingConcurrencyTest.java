package Seatsure.Backend;

import Seatsure.Backend.entity.*;
import Seatsure.Backend.enums.*;
import Seatsure.Backend.repository.*;
import Seatsure.Backend.service.BookingService;
import Seatsure.Backend.service.SeatHoldService;
import Seatsure.Backend.dto.CreateBookingRequestDto;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.util.UUID;

@SpringBootTest
class BookingConcurrencyTest {

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private ShowRepository showRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private ShowSeatRepository showSeatRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoBean
    private SeatHoldService seatHoldService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BookingItemRepository bookingItemRepository;

    @Test
    void concurrentBookingAttemptsForSameSeat() throws Exception {

        String testId = UUID.randomUUID().toString();

        TransactionTemplate tx =
                new TransactionTemplate(transactionManager);

        TestData data = tx.execute(status -> {

            Venue venue = venueRepository.save(
                    Venue.builder()
                            .name("Concurrency Test Venue")
                            .city("Mumbai")
                            .address("Test Address")
                            .capacity(100)
                            .build()
            );

            Event event = eventRepository.save(
                    Event.builder()
                            .name("Concurrency Test Event")
                            .description("Test event")
                            .category(Category.values()[0])
                            .durationMinutes(120)
                            .status(Status.values()[0])
                            .build()
            );

            LocalDateTime startTime = LocalDateTime.now().plusDays(2);

            Show show = showRepository.save(
                    Show.builder()
                            .event(event)
                            .venue(venue)
                            .startTime(startTime)
                            .endTime(startTime.plusHours(2))
                            .status(Status.values()[0])
                            .build()
            );

            Seat seat = seatRepository.save(
                    Seat.builder()
                            .venue(venue)
                            .seatNumber("CONCURRENCY-A1")
                            .rowNumber(1)
                            .seatType(SeatType.values()[0])
                            .build()
            );

            ShowSeat showSeat = showSeatRepository.save(
                    ShowSeat.builder()
                            .show(show)
                            .seat(seat)
                            .status(ShowSeatStatus.AVAILABLE)
                            .price(new BigDecimal("200.00"))
                            .build()
            );

            User user1 = userRepository.save(
                    User.builder()
                            .name("Concurrency User One")
                            .email("concurrency-one-" + testId + "@example.com")
                            .password("test-password")
                            .role(Role.values()[0])
                            .build()
            );

            User user2 = userRepository.save(
                    User.builder()
                            .name("Concurrency User Two")
                            .email("concurrency-two-" + testId + "@example.com")
                            .password("test-password")
                            .role(Role.values()[0])
                            .build()
            );

            return new TestData(show.getId(), showSeat.getId(),
                    user1.getEmail(), user2.getEmail());
        });

        assertNotNull(data);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        when(seatHoldService.isSeatHeldByUser(
                anyLong(), anyLong(), anyString()
        )).thenAnswer(invocation -> {
            ready.countDown();

            if (!start.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException(
                        "Timed out waiting for concurrent requests"
                );
            }

            return true;
        });

        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Callable<String> attempt1 = () -> attemptBooking(
                    data, data.email1(), ready, start
            );

            Callable<String> attempt2 = () -> attemptBooking(
                    data, data.email2(), ready, start
            );

            Future<String> result1 = executor.submit(attempt1);
            Future<String> result2 = executor.submit(attempt2);

            assertTrue(
                    ready.await(10, TimeUnit.SECONDS),
                    "Both booking attempts should reach the hold check"
            );

            start.countDown();

            String outcome1 = result1.get(30, TimeUnit.SECONDS);
            String outcome2 = result2.get(30, TimeUnit.SECONDS);

            System.out.println("Booking attempt 1: " + outcome1);
            System.out.println("Booking attempt 2: " + outcome2);

            long successes = List.of(outcome1, outcome2).stream()
                    .filter("SUCCESS"::equals)
                    .count();

            assertEquals(
                    1,
                    successes,
                    "Exactly one booking should succeed for the same seat"
            );
            System.out.println("ShowSeat ID tested: " + data.showSeatId());

            long activeItems = new TransactionTemplate(transactionManager)
                    .execute(status -> {
                        List<BookingItem> items =
                                bookingItemRepository.findByShowSeatId(data.showSeatId());

                        return items.stream()
                                .filter(item ->
                                        item.getBooking().getStatus() == BookingStatus.PENDING
                                                || item.getBooking().getStatus() == BookingStatus.CONFIRMED)
                                .count();
                    });

            assertEquals(
                    1L,
                    activeItems,
                    "Exactly one active booking item should exist for the contested seat"
            );
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    private String attemptBooking(
            TestData data,
            String email,
            CountDownLatch ready,
            CountDownLatch start) {

        try {
            bookingService.createBooking(
                    email,
                    CreateBookingRequestDto.builder()
                            .showId(data.showId())
                            .showSeatIds(List.of(data.showSeatId()))
                            .build()
            );

            return "SUCCESS";

        } catch (Exception exception) {
            return "FAILED: " + exception.getClass().getSimpleName()
                    + " - " + exception.getMessage();
        }
    }

    private record TestData(
            Long showId,
            Long showSeatId,
            String email1,
            String email2) {
    }
}

