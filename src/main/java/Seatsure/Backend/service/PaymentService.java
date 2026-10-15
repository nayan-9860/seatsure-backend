package Seatsure.Backend.service;

import Seatsure.Backend.dto.CreatePaymentRequestDto;
import Seatsure.Backend.dto.PaymentResponseDto;
import Seatsure.Backend.entity.*;
import Seatsure.Backend.enums.BookingStatus;
import Seatsure.Backend.enums.PaymentStatus;
import Seatsure.Backend.enums.ShowSeatStatus;
import Seatsure.Backend.exception.BookingNotFoundException;
import Seatsure.Backend.exception.SeatNotAvailableException;
import Seatsure.Backend.exception.UserNotFoundException;
import Seatsure.Backend.repository.BookingRepository;
import Seatsure.Backend.repository.PaymentRepository;
import Seatsure.Backend.repository.ShowSeatRepository;
import Seatsure.Backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final SeatHoldService seatHoldService;
    private final ShowSeatRepository showSeatRepository;
    private final ApplicationEventPublisher eventPublisher;


    public PaymentResponseDto createPayment(String email, CreatePaymentRequestDto requestDto) {

        //find user
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("user is not found with email : " + email));

        //find the booking
        Booking booking = bookingRepository.findById(requestDto.getBookingId())
                .orElseThrow(() -> new BookingNotFoundException
                        ("booking is not found with id : " + requestDto.getBookingId()));

        //authorization check:
        //The logged-in user must own the booking they are trying to pay for.
        if (!booking.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("You are not allowed to make payment for this booking");
        }

        //
        if (paymentRepository.existsByBookingId(booking.getId())) {
            throw new IllegalArgumentException(
                    "Payment already exists for this booking"
            );
        }

        //verify booking status --- only pending booking status are allowed
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalArgumentException("Payment can only be initiated for a pending booking");
        }

        //create Payment
        Payment payment = Payment.builder()
                .booking(booking)
                .amount(booking.getTotalAmount())
                .status(PaymentStatus.INITIATED)
                .paymentMethod(requestDto.getPaymentMethod())
                .build();

        //save in repository
        Payment savedPayment = paymentRepository.save(payment);

        return PaymentResponseDto.builder()
                .paymentId(savedPayment.getId())
                .bookingId(savedPayment.getBooking().getId())
                .amount(savedPayment.getAmount())
                .status(savedPayment.getStatus())
                .paymentMethod(savedPayment.getPaymentMethod())
                .transactionId(savedPayment.getTransactionId())
                .createdAt(savedPayment.getCreatedAt())
                .updatedAt(savedPayment.getUpdatedAt())
                .build();

    }


    @Transactional
    public PaymentResponseDto successPayment(String email, Long paymentId) {

        //find User
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User is not found with email : " + email));

        //find payment
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found with id : " + paymentId));

        //getBooking
        //Booking booking = payment.getBooking();
        // we used pessimistic locking here
        Long bookingId = payment.getBooking().getId();

        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(
                        "Booking not found with id: " + bookingId
                ));


        //ownership check
        if (!booking.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("You are not allowed to complete this payment");
        }

        // Payment must be INITIATED
        if (payment.getStatus() != PaymentStatus.INITIATED) {
            throw new IllegalArgumentException(
                    "Payment cannot be completed because its current status is "
                            + payment.getStatus()
            );

        }
        //Booking must still be PENDING
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalArgumentException
                    ("Booking cannot be confirmed because its current status is " + booking.getStatus());
        }

        //A booking must not be confirmed if its payment deadline has passed.
        if (booking.getExpiresAt() == null
                || !booking.getExpiresAt().isAfter(LocalDateTime.now())) {

            throw new IllegalArgumentException(
                    "Booking has expired. Please create a new booking."
            );
        }

        // Test synchronization point; no effect when the hook is unset.
        runPaymentRaceTestHook();

        // Mark each seat as BOOKED
        for (BookingItem item : booking.getBookingItems()) {

            ShowSeat showSeat = item.getShowSeat();

            if (showSeat.getStatus() != ShowSeatStatus.HELD) {
                throw new SeatNotAvailableException(
                        "Seat is no longer held for this booking: "
                                + showSeat.getId()
                );
            }

            showSeat.setStatus(ShowSeatStatus.BOOKED);
        }

        // Mark payment successful
        payment.setStatus(PaymentStatus.SUCCESS);

        // Temporary transaction ID for our simulation
        payment.setTransactionId("TXN-" + payment.getId());

        //  Confirm booking
        booking.setStatus(BookingStatus.CONFIRMED);

        //  Save both
        paymentRepository.save(payment);
        bookingRepository.save(booking);

        // Flush pending database changes before returning success
        showSeatRepository.flush();

        for (BookingItem item : booking.getBookingItems()) {
            eventPublisher.publishEvent(
                    new PaymentCommittedEvent(
                            booking.getShow().getId(),
                            item.getShowSeat().getId(),
                            booking.getUser().getId()
                    )
            );
        }

        return PaymentResponseDto.builder()
                .paymentId(payment.getId())
                .bookingId(booking.getId())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .paymentMethod(payment.getPaymentMethod())
                .transactionId(payment.getTransactionId())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();

    }


    @Transactional
    public PaymentResponseDto failurePayment(
            String email,
            Long paymentId) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UserNotFoundException(
                                "User not found with email: " + email
                        ));

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Payment not found with id: " + paymentId
                        ));

        //Booking booking = payment.getBooking();

        // Get the ID of the booking associated with this payment.
        // We need the booking ID to fetch the booking with a database lock.
        Long bookingId = payment.getBooking().getId();

        // Fetch the booking while acquiring a pessimistic write lock.
        // This uses the same lock as successPayment() and expirePendingBookings().
        // If another transaction holds this lock, this transaction must wait
        // until that transaction completes or the database reports a timeout.
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(
                        "Booking not found with id: " + bookingId
                ));


        // Check ownership
        if (!booking.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException(
                    "You are not allowed to update this payment"
            );
        }

        // Payment must currently be INITIATED
        if (payment.getStatus() != PaymentStatus.INITIATED) {
            throw new IllegalArgumentException(
                    "Payment cannot be failed because its current status is "
                            + payment.getStatus()
            );
        }

        // Mark payment as failed
        payment.setStatus(PaymentStatus.FAILED);

        // Cancel the associated booking
        booking.setStatus(BookingStatus.CANCELLED);

        // Release Redis holds belonging to this booking's user
        /*
        Long showId = booking.getShow().getId();
        Long userId = booking.getUser().getId();

        booking.getBookingItems().forEach(item ->
            seatHoldService.releaseSeatIfOwned(showId, item.getShowSeat().getId(), userId)
        );*/
        for (BookingItem item : booking.getBookingItems()) {

            ShowSeat showSeat = item.getShowSeat();

            // Release the PostgreSQL reservation
            if (showSeat.getStatus() == ShowSeatStatus.HELD) {
                showSeat.setStatus(ShowSeatStatus.AVAILABLE);
            }

            // Release the Redis hold if it belongs to this user
//            seatHoldService.releaseSeatIfOwned(
//                    booking.getShow().getId(),
//                    showSeat.getId(),
//                    booking.getUser().getId()
//            );


            // Schedule Redis cleanup after the database transaction commits.
            // If the transaction rolls back, the AFTER_COMMIT listener won't run.
            eventPublisher.publishEvent(
                    new SeatHoldReleaseEvent(
                            booking.getShow().getId(),
                            showSeat.getId(),
                            booking.getUser().getId()
                    )
            );

        }

        Payment savedPayment = paymentRepository.save(payment);
        bookingRepository.save(booking);

        return PaymentResponseDto.builder()
                .paymentId(savedPayment.getId())
                .bookingId(booking.getId())
                .amount(savedPayment.getAmount())
                .status(savedPayment.getStatus())
                .paymentMethod(savedPayment.getPaymentMethod())
                .transactionId(savedPayment.getTransactionId())
                .createdAt(savedPayment.getCreatedAt())
                .updatedAt(savedPayment.getUpdatedAt())
                .build();
    }


    @Transactional(readOnly = true)
    public PaymentResponseDto getPayment(
            String email,
            Long paymentId) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UserNotFoundException(
                                "User not found with email: " + email
                        ));

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Payment not found with id: " + paymentId
                        ));

        Booking booking = payment.getBooking();

        if (!booking.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException(
                    "You are not allowed to view this payment"
            );
        }

        return PaymentResponseDto.builder()
                .paymentId(payment.getId())
                .bookingId(booking.getId())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .paymentMethod(payment.getPaymentMethod())
                .transactionId(payment.getTransactionId())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }


//
//    /*
//     * Test-only synchronization hook.
//     * It remains null during normal application execution.
//     */
    private volatile Runnable paymentRaceTestHook;

    public void setPaymentRaceTestHook(Runnable hook) {
        this.paymentRaceTestHook = hook;
    }

    private void runPaymentRaceTestHook() {
        Runnable hook = paymentRaceTestHook;

        if (hook != null) {
            hook.run();
        }
    }


}