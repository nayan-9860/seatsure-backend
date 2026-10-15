
package Seatsure.Backend;

import Seatsure.Backend.entity.Booking;
import Seatsure.Backend.entity.BookingItem;
import Seatsure.Backend.entity.Event;
import Seatsure.Backend.entity.Payment;
import Seatsure.Backend.entity.Seat;
import Seatsure.Backend.entity.Show;
import Seatsure.Backend.entity.ShowSeat;
import Seatsure.Backend.entity.User;
import Seatsure.Backend.entity.Venue;

import Seatsure.Backend.enums.BookingStatus;
import Seatsure.Backend.enums.Category;
import Seatsure.Backend.enums.PaymentMethod;
import Seatsure.Backend.enums.PaymentStatus;
import Seatsure.Backend.enums.Role;
import Seatsure.Backend.enums.SeatType;
import Seatsure.Backend.enums.ShowSeatStatus;
import Seatsure.Backend.enums.Status;

import Seatsure.Backend.repository.BookingRepository;
import Seatsure.Backend.repository.EventRepository;
import Seatsure.Backend.repository.PaymentRepository;
import Seatsure.Backend.repository.SeatRepository;
import Seatsure.Backend.repository.ShowRepository;
import Seatsure.Backend.repository.ShowSeatRepository;
import Seatsure.Backend.repository.UserRepository;
import Seatsure.Backend.repository.VenueRepository;

import Seatsure.Backend.service.BookingService;
import Seatsure.Backend.service.PaymentService;
import Seatsure.Backend.service.SeatHoldService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class BookingExpiryPaymentRaceTest {


    private final java.util.List<TestData> createdTestData =
            new java.util.concurrent.CopyOnWriteArrayList<>();

    @AfterEach
    void cleanupRedisHolds() {
        for (TestData data : createdTestData) {
            seatHoldService.releaseSeatIfOwned(
                    data.showId(),
                    data.showSeatId(),
                    data.userId()
            );
        }

        createdTestData.clear();
    }

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
    private BookingRepository bookingRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private SeatHoldService seatHoldService;

    @Autowired
    private PlatformTransactionManager transactionManager;

//    @Autowired
//    private RedisTemplate<String, String> redisTemplate;

    private record TestData(
            Long userId,
            String userEmail,
            Long bookingId,
            Long paymentId,
            Long showId,
            Long showSeatId
    ) {
    }

    private TestData createTestData() {

        TransactionTemplate tx =
                new TransactionTemplate(transactionManager);

        return tx.execute(status -> {

            // 1. Create test user
            User user = userRepository.save(
                    User.builder()
                            .name("Race Test User")
                            .email("race-test-" + UUID.randomUUID()
                                    + "@example.com")
                            .password("test-only-password")
                            .role(Role.USER)
                            .build()
            );

            // 2. Create venue
            Venue venue = venueRepository.save(
                    Venue.builder()
                            .name("Race Test Venue")
                            .city("Mumbai")
                            .address("Test Address")
                            .capacity(100)
                            .build()
            );

            // 3. Create event
            Event event = eventRepository.save(
                    Event.builder()
                            .name("Race Test Event")
                            .description("Test event for booking expiry race")
                            .category(Category.values()[0])
                            .durationMinutes(120)
                            .status(Status.values()[0])
                            .build()
            );

            // 4. Create show
            LocalDateTime startTime =
                    LocalDateTime.now().plusDays(2);

            Show show = showRepository.save(
                    Show.builder()
                            .event(event)
                            .venue(venue)
                            .startTime(startTime)
                            .endTime(startTime.plusHours(2))
                            .status(Status.values()[0])
                            .build()
            );

            // 5. Create physical seat
            Seat seat = seatRepository.save(
                    Seat.builder()
                            .venue(venue)
                            .seatNumber("RACE-A1")
                            .rowNumber(1)
                            .seatType(SeatType.values()[0])
                            .build()
            );

            // 6. Create held show-seat
            ShowSeat showSeat = showSeatRepository.save(
                    ShowSeat.builder()
                            .show(show)
                            .seat(seat)
                            .status(ShowSeatStatus.HELD)
                            .price(new BigDecimal("200.00"))
                            .build()
            );

            // 7. Create pending booking
            Booking booking = Booking.builder()
                    .user(user)
                    .show(show)
                    .status(BookingStatus.PENDING)
                    .totalAmount(new BigDecimal("200.00"))
                    .expiresAt(LocalDateTime.now().plusMinutes(5))
                    .bookingItems(new ArrayList<>())
                    .build();

            // 8. Connect booking and booking item
            BookingItem bookingItem = BookingItem.builder()
                    .booking(booking)
                    .showSeat(showSeat)
                    .price(new BigDecimal("200.00"))
                    .build();

            booking.getBookingItems().add(bookingItem);

            booking = bookingRepository.saveAndFlush(booking);

            // 9. Create initiated payment
            Payment payment = paymentRepository.saveAndFlush(
                    Payment.builder()
                            .booking(booking)
                            .amount(booking.getTotalAmount())
                            .status(PaymentStatus.INITIATED)
                            .paymentMethod(PaymentMethod.UPI)
                            .build()
            );

            // 10. Flush and return generated IDs
            showSeatRepository.flush();


            TestData data = new TestData(
                    user.getId(),
                    user.getEmail(),
                    booking.getId(),
                    payment.getId(),
                    show.getId(),
                    showSeat.getId()
            );

            createdTestData.add(data);
            return data;
        });
    }

    @Test
    void paymentAndExpiryRaceMustNotProduceInconsistentState() {

        TestData data = createTestData();

        TransactionTemplate tx =
                new TransactionTemplate(transactionManager);

        // Make the booking expired.
        tx.executeWithoutResult(status -> {
            Booking booking = bookingRepository.findById(data.bookingId())
                    .orElseThrow();

            booking.setExpiresAt(LocalDateTime.now().minusSeconds(1));
            bookingRepository.saveAndFlush(booking);
        });

        // Run expiry.
        bookingService.expirePendingBookings();

        Booking bookingAfterExpiry =
                bookingRepository.findById(data.bookingId()).orElseThrow();

        Payment paymentAfterExpiry =
                paymentRepository.findById(data.paymentId()).orElseThrow();

        ShowSeat seatAfterExpiry =
                showSeatRepository.findById(data.showSeatId()).orElseThrow();

        assertEquals(
                BookingStatus.CANCELLED,
                bookingAfterExpiry.getStatus()
        );

        assertEquals(
                PaymentStatus.INITIATED,
                paymentAfterExpiry.getStatus()
        );

        assertEquals(
                ShowSeatStatus.AVAILABLE,
                seatAfterExpiry.getStatus()
        );
    }

    @Test
    void paymentMustBeRejectedAfterBookingExpires() {

        TestData data = createTestData();

        TransactionTemplate tx =
                new TransactionTemplate(transactionManager);

        // Expire the booking before attempting payment.
        tx.executeWithoutResult(status -> {
            Booking booking = bookingRepository.findById(data.bookingId())
                    .orElseThrow();

            booking.setExpiresAt(LocalDateTime.now().minusSeconds(1));
            bookingRepository.saveAndFlush(booking);
        });

        assertThrows(
                RuntimeException.class,
                () -> paymentService.successPayment(
                        data.userEmail(),
                        data.paymentId()
                )
        );

        Booking bookingAfterAttempt =
                bookingRepository.findById(data.bookingId()).orElseThrow();

        Payment paymentAfterAttempt =
                paymentRepository.findById(data.paymentId()).orElseThrow();

        ShowSeat seatAfterAttempt =
                showSeatRepository.findById(data.showSeatId()).orElseThrow();

        assertEquals(
                BookingStatus.PENDING,
                bookingAfterAttempt.getStatus()
        );

        assertEquals(
                PaymentStatus.INITIATED,
                paymentAfterAttempt.getStatus()
        );

        assertEquals(
                ShowSeatStatus.HELD,
                seatAfterAttempt.getStatus()
        );
    }



    @Test
    void concurrentPaymentAndExpiryMustPreserveConsistentState() throws Exception {

        TestData data = createTestData();

        TransactionTemplate tx =
                new TransactionTemplate(transactionManager);

        // Step 1: Create a real Redis hold for the test user.
        tx.executeWithoutResult(status -> {
            ShowSeat seat = showSeatRepository.findById(data.showSeatId())
                    .orElseThrow();

            seat.setStatus(ShowSeatStatus.AVAILABLE);
            showSeatRepository.saveAndFlush(seat);
        });

        seatHoldService.holdSeat(
                data.showId(),
                data.showSeatId(),
                data.userEmail()
        );

        // Keep PostgreSQL's seat state consistent with the Redis hold.
        // Give the booking a short deadline so it expires during this test.
        tx.executeWithoutResult(status -> {
            ShowSeat seat = showSeatRepository.findById(data.showSeatId())
                    .orElseThrow();

            seat.setStatus(ShowSeatStatus.HELD);
            showSeatRepository.saveAndFlush(seat);

            Booking booking = bookingRepository.findById(data.bookingId())
                    .orElseThrow();

            booking.setExpiresAt(LocalDateTime.now().plusSeconds(2));
            bookingRepository.saveAndFlush(booking);
        });

        assertTrue(
                seatHoldService.isSeatHeldByUser(
                        data.showId(),
                        data.showSeatId(),
                        data.userEmail()
                ),
                "Redis hold must exist before the test"
        );

        CountDownLatch paymentValidated = new CountDownLatch(1);
        CountDownLatch allowPaymentToContinue = new CountDownLatch(1);
        CountDownLatch expiryStarted = new CountDownLatch(1);

        AtomicReference<String> paymentOutcome = new AtomicReference<>();
        AtomicReference<String> expiryOutcome = new AtomicReference<>();

        // Step 2: Pause payment after its validation.
        // successPayment() has already acquired the booking lock at this point.
        paymentService.setPaymentRaceTestHook(() -> {
            paymentValidated.countDown();

            awaitLatch(
                    allowPaymentToContinue,
                    "Timed out waiting to resume payment"
            );
        });

        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            // Start payment on a separate thread.
            Future<?> paymentFuture = executor.submit(() -> {
                try {
                    paymentService.successPayment(
                            data.userEmail(),
                            data.paymentId()
                    );

                    paymentOutcome.set("SUCCESS");

                } catch (RuntimeException e) {
                    paymentOutcome.set(
                            "FAILED: " + e.getClass().getSimpleName()
                                    + " - " + e.getMessage()
                    );
                    throw e;
                }
            });

            // Wait until payment holds the booking lock and reaches the hook.
            awaitLatch(
                    paymentValidated,
                    "Payment did not reach the synchronization point"
            );

            // Let the booking deadline pass naturally while payment is paused.
            Thread.sleep(3000);

            // Step 3: Start expiry on another thread.
            // Expiry should find the expired pending booking and request its lock.
            Future<Integer> expiryFuture = executor.submit(() -> {
                expiryStarted.countDown();
                return bookingService.expirePendingBookings();
            });

            // Ensure the expiry worker has started before resuming payment.
            awaitLatch(
                    expiryStarted,
                    "Expiry worker did not start"
            );

            // Allow payment to finish and commit its changes.
            allowPaymentToContinue.countDown();

            // Wait for payment and expiry to finish.
            paymentFuture.get(15, TimeUnit.SECONDS);

            int expiredCount = expiryFuture.get(15, TimeUnit.SECONDS);
            expiryOutcome.set("SUCCESS: expired " + expiredCount);

            // Step 4: Verify the final database state.
            Booking finalBooking =
                    bookingRepository.findById(data.bookingId()).orElseThrow();

            Payment finalPayment =
                    paymentRepository.findById(data.paymentId()).orElseThrow();

            ShowSeat finalSeat =
                    showSeatRepository.findById(data.showSeatId()).orElseThrow();

            System.out.println("Payment outcome: " + paymentOutcome.get());
            System.out.println("Expiry outcome: " + expiryOutcome.get());
            System.out.println("Final booking: " + finalBooking.getStatus());
            System.out.println("Final payment: " + finalPayment.getStatus());
            System.out.println("Final seat: " + finalSeat.getStatus());

            assertEquals(
                    "SUCCESS",
                    paymentOutcome.get(),
                    "Payment should complete successfully"
            );

            assertEquals(
                    0,
                    expiredCount,
                    "Expiry must skip the booking after payment confirms it"
            );

            assertEquals(
                    BookingStatus.CONFIRMED,
                    finalBooking.getStatus()
            );

            assertEquals(
                    PaymentStatus.SUCCESS,
                    finalPayment.getStatus()
            );

            assertEquals(
                    ShowSeatStatus.BOOKED,
                    finalSeat.getStatus()
            );

            assertFalse(
                    seatHoldService.isSeatHeldByUser(
                            data.showId(),
                            data.showSeatId(),
                            data.userEmail()
                    ),
                    "Redis hold should be released after payment commits"
            );

        } finally {
            // Always unblock payment if an assertion or timeout occurs.
            allowPaymentToContinue.countDown();

            // Remove the test hook so it cannot affect other tests.
            paymentService.setPaymentRaceTestHook(null);

            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }


    @Test
    void successfulPaymentMustNotBeExpired() {

        TestData data = createTestData();

        TransactionTemplate tx =
                new TransactionTemplate(transactionManager);

        // Step 1: Create a real Redis hold.
        tx.executeWithoutResult(status -> {
            ShowSeat seat = showSeatRepository.findById(data.showSeatId())
                    .orElseThrow();

            seat.setStatus(ShowSeatStatus.AVAILABLE);
            showSeatRepository.saveAndFlush(seat);
        });

        seatHoldService.holdSeat(
                data.showId(),
                data.showSeatId(),
                data.userEmail()
        );

        assertTrue(
                seatHoldService.isSeatHeldByUser(
                        data.showId(),
                        data.showSeatId(),
                        data.userEmail()
                ),
                "Redis hold must exist before payment"
        );

        tx.executeWithoutResult(status -> {
            ShowSeat seat = showSeatRepository.findById(data.showSeatId())
                    .orElseThrow();

            seat.setStatus(ShowSeatStatus.HELD);
            showSeatRepository.saveAndFlush(seat);
        });

        // Step 2: Ensure the booking has a valid payment deadline.
        tx.executeWithoutResult(status -> {
            Booking booking = bookingRepository.findById(data.bookingId())
                    .orElseThrow();

            booking.setExpiresAt(LocalDateTime.now().plusMinutes(5));
            bookingRepository.saveAndFlush(booking);
        });

        // Step 3: Complete payment first.
        paymentService.successPayment(
                data.userEmail(),
                data.paymentId()
        );

        // Verify the AFTER_COMMIT listener removed the Redis hold.
        assertFalse(
                seatHoldService.isSeatHeldByUser(
                        data.showId(),
                        data.showSeatId(),
                        data.userEmail()
                ),
                "Redis hold should be released after successful payment commits"
        );

        // Step 4: Run expiry after payment has completed.
        int expiredCount = bookingService.expirePendingBookings();

        Booking finalBooking =
                bookingRepository.findById(data.bookingId()).orElseThrow();

        Payment finalPayment =
                paymentRepository.findById(data.paymentId()).orElseThrow();

        ShowSeat finalSeat =
                showSeatRepository.findById(data.showSeatId()).orElseThrow();

        assertEquals(
                0,
                expiredCount,
                "Expiry must not select a confirmed booking"
        );

        assertEquals(
                BookingStatus.CONFIRMED,
                finalBooking.getStatus()
        );

        assertEquals(
                PaymentStatus.SUCCESS,
                finalPayment.getStatus()
        );

        assertEquals(
                ShowSeatStatus.BOOKED,
                finalSeat.getStatus()
        );

        assertFalse(
                seatHoldService.isSeatHeldByUser(
                        data.showId(),
                        data.showSeatId(),
                        data.userEmail()
                ),
                "Redis hold should remain absent after expiry runs"
        );
    }


    @Test
    void concurrentPaymentMustWinBeforeExpiryRuns() throws Exception {

        TestData data = createTestData();

        TransactionTemplate tx =
                new TransactionTemplate(transactionManager);

        // 1. Create a real Redis hold.
        tx.executeWithoutResult(status -> {
            ShowSeat seat = showSeatRepository.findById(data.showSeatId())
                    .orElseThrow();

            seat.setStatus(ShowSeatStatus.AVAILABLE);
            showSeatRepository.saveAndFlush(seat);
        });

        seatHoldService.holdSeat(
                data.showId(),
                data.showSeatId(),
                data.userEmail()
        );

        tx.executeWithoutResult(status -> {
            ShowSeat seat = showSeatRepository.findById(data.showSeatId())
                    .orElseThrow();

            seat.setStatus(ShowSeatStatus.HELD);
            showSeatRepository.saveAndFlush(seat);

            Booking booking = bookingRepository.findById(data.bookingId())
                    .orElseThrow();

            booking.setExpiresAt(LocalDateTime.now().plusMinutes(5));
            bookingRepository.saveAndFlush(booking);
        });

        assertTrue(
                seatHoldService.isSeatHeldByUser(
                        data.showId(),
                        data.showSeatId(),
                        data.userEmail()
                )
        );

        CountDownLatch paymentCommitted = new CountDownLatch(1);
        ExecutorService executor = Executors.newSingleThreadExecutor();

        try {
            Future<?> paymentFuture = executor.submit(() -> {
                try {
                    paymentService.successPayment(
                            data.userEmail(),
                            data.paymentId()
                    );
                } finally {
                    // successPayment returns only after its Spring
                    // transaction has committed successfully.
                    paymentCommitted.countDown();
                }
            });

            // 2. Wait until the payment operation has returned.
            awaitLatch(
                    paymentCommitted,
                    "Payment did not finish within the expected time"
            );

            // 3. Propagate any exception from the payment thread.
            paymentFuture.get(10, TimeUnit.SECONDS);

            // 4. Run expiry only after payment has completed.
            int expiredCount = bookingService.expirePendingBookings();

            assertEquals(0, expiredCount);

            Booking finalBooking =
                    bookingRepository.findById(data.bookingId()).orElseThrow();

            Payment finalPayment =
                    paymentRepository.findById(data.paymentId()).orElseThrow();

            ShowSeat finalSeat =
                    showSeatRepository.findById(data.showSeatId()).orElseThrow();

            assertEquals(BookingStatus.CONFIRMED, finalBooking.getStatus());
            assertEquals(PaymentStatus.SUCCESS, finalPayment.getStatus());
            assertEquals(ShowSeatStatus.BOOKED, finalSeat.getStatus());

            assertFalse(
                    seatHoldService.isSeatHeldByUser(
                            data.showId(),
                            data.showSeatId(),
                            data.userEmail()
                    ),
                    "Redis hold should be absent after payment commits"
            );

        } finally {
            executor.shutdownNow();
        }
    }



    @Test
    void duplicateSuccessfulPaymentMustBeRejected() {

        // Arrange: create a pending booking and an initiated payment.
        TestData data = createTestData();

        TransactionTemplate tx =
                new TransactionTemplate(transactionManager);

        // Create the Redis hold required by the payment flow.

        // Remove any stale Redis hold left by a previous test.
//        seatHoldService.releaseSeatIfOwned(
//                data.showId(),
//                data.showSeatId(),
//                data.userId()
//        );

        // Make the database seat available before acquiring the hold.
        tx.executeWithoutResult(status -> {
            ShowSeat seat = showSeatRepository.findById(data.showSeatId())
                    .orElseThrow();

            seat.setStatus(ShowSeatStatus.AVAILABLE);
            showSeatRepository.saveAndFlush(seat);
        });

        // Create the Redis hold required by the payment flow.
        seatHoldService.holdSeat(
                data.showId(),
                data.showSeatId(),
                data.userEmail()
        );

        // Keep the database seat state consistent with the Redis hold.
        tx.executeWithoutResult(status -> {
            ShowSeat seat = showSeatRepository.findById(data.showSeatId())
                    .orElseThrow();

            seat.setStatus(ShowSeatStatus.HELD);
            showSeatRepository.saveAndFlush(seat);
        });

        // Act: complete the first payment successfully.
        paymentService.successPayment(
                data.userEmail(),
                data.paymentId()
        );

        // Assert: a second attempt must be rejected.
        assertThrows(
                IllegalArgumentException.class,
                () -> paymentService.successPayment(
                        data.userEmail(),
                        data.paymentId()
                )
        );

        // Verify the original successful payment remains unchanged.
        Booking finalBooking =
                bookingRepository.findById(data.bookingId()).orElseThrow();

        Payment finalPayment =
                paymentRepository.findById(data.paymentId()).orElseThrow();

        ShowSeat finalSeat =
                showSeatRepository.findById(data.showSeatId()).orElseThrow();

        assertEquals(BookingStatus.CONFIRMED, finalBooking.getStatus());
        assertEquals(PaymentStatus.SUCCESS, finalPayment.getStatus());
        assertEquals(ShowSeatStatus.BOOKED, finalSeat.getStatus());
    }


    @Test
    void anotherUserMustNotCompletePayment() {

        // Arrange: create a booking and payment owned by User A.
        TestData data = createTestData();

        // Create User B.
        User anotherUser = userRepository.saveAndFlush(
                User.builder()
                        .name("Another Test User")
                        .email("other-user-" + UUID.randomUUID()
                                + "@example.com")
                        .password("test-only-password")
                        .role(Role.USER)
                        .build()
        );

        // Act and assert: User B must not be allowed to complete
        // User A's payment.
        assertThrows(
                IllegalArgumentException.class,
                () -> paymentService.successPayment(
                        anotherUser.getEmail(),
                        data.paymentId()
                )
        );

        // Verify that the unauthorized attempt changed nothing.
        Booking booking =
                bookingRepository.findById(data.bookingId()).orElseThrow();

        Payment payment =
                paymentRepository.findById(data.paymentId()).orElseThrow();

        ShowSeat seat =
                showSeatRepository.findById(data.showSeatId()).orElseThrow();

        assertEquals(BookingStatus.PENDING, booking.getStatus());
        assertEquals(PaymentStatus.INITIATED, payment.getStatus());
        assertEquals(ShowSeatStatus.HELD, seat.getStatus());
    }


    @Test
    void cancellingPendingBookingMustReleaseRedisHoldAndSeat() {

        // Arrange: create a pending booking and its seat.
        TestData data = createTestData();

        TransactionTemplate tx =
                new TransactionTemplate(transactionManager);

        // Create a Redis hold for the booking's user.
        tx.executeWithoutResult(status -> {
            ShowSeat seat = showSeatRepository.findById(data.showSeatId())
                    .orElseThrow();

            seat.setStatus(ShowSeatStatus.AVAILABLE);
            showSeatRepository.saveAndFlush(seat);
        });

        seatHoldService.holdSeat(
                data.showId(),
                data.showSeatId(),
                data.userEmail()
        );

        // Keep the database seat state consistent with the Redis hold.
        tx.executeWithoutResult(status -> {
            ShowSeat seat = showSeatRepository.findById(data.showSeatId())
                    .orElseThrow();

            seat.setStatus(ShowSeatStatus.HELD);
            showSeatRepository.saveAndFlush(seat);
        });

        // Verify that the hold exists before cancellation.
        assertTrue(
                seatHoldService.isSeatHeldByUser(
                        data.showId(),
                        data.showSeatId(),
                        data.userEmail()
                )
        );

        // Act: cancel the pending booking.
        bookingService.cancelBooking(
                data.userEmail(),
                data.bookingId()
        );

        // Assert: the booking and seat should be updated.
        Booking finalBooking =
                bookingRepository.findById(data.bookingId()).orElseThrow();

        ShowSeat finalSeat =
                showSeatRepository.findById(data.showSeatId()).orElseThrow();

        assertEquals(BookingStatus.CANCELLED, finalBooking.getStatus());
        assertEquals(ShowSeatStatus.AVAILABLE, finalSeat.getStatus());

        // Redis hold should be released after the transaction commits.
        assertFalse(
                seatHoldService.isSeatHeldByUser(
                        data.showId(),
                        data.showSeatId(),
                        data.userEmail()
                ),
                "Redis hold should be released after cancellation commits"
        );
    }



    @Test
    void anotherUserMustNotCancelSomeoneElsesBooking() {
        // Arrange: create a booking owned by the original user.
        TestData data = createTestData();

        // Create a different user.
        User anotherUser = userRepository.saveAndFlush(
                User.builder()
                        .name("Another Test User")
                        .email("other-user-" + UUID.randomUUID() + "@example.com")
                        .password("test-only-password")
                        .role(Role.USER)
                        .build()
        );

        // Act & Assert: the other user must not be allowed to cancel it.
        assertThrows(IllegalArgumentException.class, () ->
                bookingService.cancelBooking(
                        anotherUser.getEmail(),
                        data.bookingId()
                )
        );

        // Verify that the original booking and seat remain unchanged.
        Booking booking = bookingRepository.findById(data.bookingId()).orElseThrow();
        ShowSeat seat = showSeatRepository.findById(data.showSeatId()).orElseThrow();

        assertEquals(BookingStatus.PENDING, booking.getStatus());
        assertEquals(ShowSeatStatus.HELD, seat.getStatus());
    }




    @Test
    void cancellingBookingMustNotReleaseAnotherUsersRedisHold() {
        TestData data = createTestData();

        User anotherUser = userRepository.saveAndFlush(
                User.builder()
                        .name("Another Test User")
                        .email("other-user-" + UUID.randomUUID() + "@example.com")
                        .password("test-only-password")
                        .role(Role.USER)
                        .build()
        );

        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        tx.executeWithoutResult(status -> {
            ShowSeat seat = showSeatRepository.findById(data.showSeatId())
                    .orElseThrow();

            seat.setStatus(ShowSeatStatus.AVAILABLE);
            showSeatRepository.saveAndFlush(seat);
        });

        // User B acquires the Redis hold.
        seatHoldService.holdSeat(
                data.showId(),
                data.showSeatId(),
                anotherUser.getEmail()
        );

        try {
            tx.executeWithoutResult(status -> {
                ShowSeat seat = showSeatRepository.findById(data.showSeatId())
                        .orElseThrow();

                seat.setStatus(ShowSeatStatus.HELD);
                showSeatRepository.saveAndFlush(seat);
            });

            assertTrue(seatHoldService.isSeatHeldByUser(
                    data.showId(),
                    data.showSeatId(),
                    anotherUser.getEmail()
            ));

            // User A cancels their booking.
            bookingService.cancelBooking(data.userEmail(), data.bookingId());

            Booking booking = bookingRepository.findById(data.bookingId())
                    .orElseThrow();

            ShowSeat seat = showSeatRepository.findById(data.showSeatId())
                    .orElseThrow();

            assertEquals(BookingStatus.CANCELLED, booking.getStatus());
            assertEquals(ShowSeatStatus.AVAILABLE, seat.getStatus());

            // User B's Redis hold must remain intact.
            assertTrue(
                    seatHoldService.isSeatHeldByUser(
                            data.showId(),
                            data.showSeatId(),
                            anotherUser.getEmail()
                    ),
                    "Cancelling User A's booking must not release User B's Redis hold"
            );
        } finally {
            // Clean up only User B's hold; never delete another owner's hold.
            seatHoldService.releaseSeatIfOwned(
                    data.showId(),
                    data.showSeatId(),
                    anotherUser.getId()
            );
        }
    }

    private void awaitLatch(CountDownLatch latch, String message) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException(message);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(
                    "Test synchronization interrupted", e
            );
        }
    }

}
