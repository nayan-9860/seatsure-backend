package Seatsure.Backend.service;

import Seatsure.Backend.dto.BookingItemResponseDto;
import Seatsure.Backend.dto.BookingResponseDto;
import Seatsure.Backend.dto.CreateBookingRequestDto;
import Seatsure.Backend.entity.*;
import Seatsure.Backend.enums.BookingStatus;
import Seatsure.Backend.enums.ShowSeatStatus;
import Seatsure.Backend.exception.*;
import Seatsure.Backend.repository.BookingRepository;
import Seatsure.Backend.repository.ShowRepository;
import Seatsure.Backend.repository.ShowSeatRepository;
import Seatsure.Backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final UserRepository userRepository;
    private final ShowRepository showRepository;
    private final ShowSeatRepository showSeatRepository;
    private final BookingRepository bookingRepository;
    private final SeatHoldService seatHoldService;
    private final ApplicationEventPublisher eventPublisher;
    private final EntityManager entityManager;

    @Transactional
    public BookingResponseDto createBooking(String email , CreateBookingRequestDto requestDto){

        //find user
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("user is not found"));

        //find show
        Show show = showRepository.findById(requestDto.getShowId())
                .orElseThrow(() -> new ShowNotFoundException("show is not found"));

        //find selected show seats
//        List<ShowSeat> showSeats = showSeatRepository.
//                findAllById(requestDto.getShowSeatIds());
        List<ShowSeat> showSeats =
                showSeatRepository.findByShowIdAndIdIn(
                        show.getId(),
                        requestDto.getShowSeatIds()
                );


        //make sure all requested seats exists
        if(showSeats.size() != requestDto.getShowSeatIds().size()){
            throw new RuntimeException("One or more selected seats do not exist");
        }

        //make sure seats belong to this show
        for (ShowSeat showSeat : showSeats){

            if(!showSeat.getShow().getId().equals(show.getId())){
                throw new ShowSeatNotFoundException(" Selected seat does not belong to this show");
            }
        }

        //make sure all seats are available
        for(ShowSeat showSeat : showSeats){

            if(showSeat.getStatus() != ShowSeatStatus.AVAILABLE){
                throw new SeatNotAvailableException(
                        "Seat " +
                        showSeat.getSeat().getSeatNumber() +
                        " is not available");
            }

            boolean heldByCurrentUser = seatHoldService.isSeatHeldByUser(
                    show.getId(),
                    showSeat.getId(),
                    email
            );

            if (!heldByCurrentUser) {
                throw new SeatNotAvailableException(
                        "You must hold seat " +
                                showSeat.getSeat().getSeatNumber() +
                                " before booking it"
                );
            }
        }

        //  Calculate total amount
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (ShowSeat showSeat : showSeats) {
            totalAmount = totalAmount.add(
                    showSeat.getPrice()
            );
        }

        //create booking
        Booking booking = Booking.builder()
                .user(user)
                .show(show)
                .status(BookingStatus.PENDING)
                .totalAmount(totalAmount)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .bookingItems(new ArrayList<>())
                .build();

        // 9. Create BookingItems
        for (ShowSeat showSeat : showSeats) {

            BookingItem bookingItem = BookingItem.builder()
                    .booking(booking)
                    .showSeat(showSeat)
                    .price(showSeat.getPrice())
                    .build();

            booking.getBookingItems().add(bookingItem);

            // Temporarily mark seat as booked
            //showSeat.setStatus(ShowSeatStatus.BOOKED);
            // Reserve the seat while payment is pending
            showSeat.setStatus(ShowSeatStatus.HELD);
        }

        Booking savedBooking =
                bookingRepository.saveAndFlush(booking);

        // Persist the ShowSeat status changes and detect version conflicts
        showSeatRepository.flush();

        // 11. Return response
        return mapToResponse(savedBooking);
    }


    @Transactional
    public int expirePendingBookings() {

        LocalDateTime currentTime = LocalDateTime.now();

        List<Booking> expiredBookings =
                bookingRepository.findByStatusAndExpiresAtBefore(
                        BookingStatus.PENDING,
                        currentTime
                );

        int expiredCount = 0;

        for (Booking expiredBooking : expiredBookings) {

            // Acquire the same booking lock used by the payment flow.
            Booking booking = bookingRepository.findByIdForUpdate(
                    expiredBooking.getId()
            ).orElse(null);

            // Refresh the managed entity from the database while acquiring
// the pessimistic write lock. This prevents us from relying on
// stale booking state loaded by the initial expiry query.
            if (booking != null) {
                entityManager.refresh(booking, LockModeType.PESSIMISTIC_WRITE);
            }

            // The booking may have been deleted or changed by another transaction.
            if (booking == null) {
                continue;
            }

            // Recheck status and expiry after acquiring the lock.
            // Another transaction may have completed payment while we were waiting.
            if (booking.getStatus() != BookingStatus.PENDING
                    || booking.getExpiresAt() == null
                    || !booking.getExpiresAt().isBefore(currentTime)) {
                continue;
            }

            // Cancel the still-expired booking.
            booking.setStatus(BookingStatus.CANCELLED);

            for (BookingItem item : booking.getBookingItems()) {

                ShowSeat showSeat = item.getShowSeat();

                if (showSeat.getStatus() == ShowSeatStatus.HELD) {
                    showSeat.setStatus(ShowSeatStatus.AVAILABLE);
                }

//                // Release only the Redis hold owned by this booking's user.
//                seatHoldService.releaseSeatIfOwned(
//                        booking.getShow().getId(),
//                        showSeat.getId(),
//                        booking.getUser().getId()
//                );

                // Request Redis cleanup after the PostgreSQL transaction commits.
                // If this transaction rolls back, the AFTER_COMMIT listener won't run.
                eventPublisher.publishEvent(
                        new SeatHoldReleaseEvent(
                                booking.getShow().getId(),
                                showSeat.getId(),
                                booking.getUser().getId()
                        )
                );
            }

            expiredCount++;
        }

        // Flush seat changes while the transaction is active.
        showSeatRepository.flush();

        return expiredCount;
    }


    private BookingResponseDto mapToResponse(Booking booking) {

    //Your method is basically saying:
        //"Take this Booking entity. For every BookingItem inside it,
        // extract the booking item ID, show-seat ID, physical seat information,
        // and booking-time price. Convert those into BookingItemResponseDto objects.
        // Then put those items together with the booking ID, show ID, status, total amount, and timestamps,
        // and return a BookingResponseDto."

        List<BookingItemResponseDto> items =
                booking.getBookingItems()
                        .stream()
                        .map(item ->
                                BookingItemResponseDto.builder()
                                        .bookingItemId(item.getId())
                                        .showSeatId(
                                                item.getShowSeat().getId()
                                        )
                                        .seatNumber(
                                                item.getShowSeat()
                                                        .getSeat()
                                                        .getSeatNumber()
                                        )
                                        .rowNumber(
                                                item.getShowSeat()
                                                        .getSeat()
                                                        .getRowNumber()
                                        )
                                        .seatType(
                                                item.getShowSeat()
                                                        .getSeat()
                                                        .getSeatType()
                                        )
                                        .price(item.getPrice())
                                        .build()
                        )
                        .toList();

        return BookingResponseDto.builder()
                .bookingId(booking.getId())
                .showId(booking.getShow().getId())
                .status(booking.getStatus())
                .totalAmount(booking.getTotalAmount())
                .expiresAt(booking.getExpiresAt())
                .bookingItems(items)
                .createdAt(booking.getCreatedAt())
                .updatedAt(booking.getUpdatedAt())
                .build();
    }


    @Transactional(readOnly = true)
    public List<BookingResponseDto> getMyBookings(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UserNotFoundException(
                                "User not found with email: " + email
                        )
                );

        List<Booking> bookings = bookingRepository.findByUserId(user.getId());

        return bookings.stream()
                .map(booking -> mapToResponse(booking))
                .toList();
    }


    @Transactional
    public BookingResponseDto cancelBooking(String email, Long bookingId){

        //find user
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("user is not found with email : " + email));

        //find booking associated with user

        // Fetch the booking using a pessimistic write lock.
        // Payment, expiry, failure, and cancellation should serialize
        // their changes to the same booking row.
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(
                        "Booking not found with id: " + bookingId
                ));


        //check user is allowed to cancel the booking
        //verify booking belongs to current user
        if(!booking.getUser().getId().equals(user.getId())){
            throw new IllegalArgumentException("You are not allowed to cancel this booking");
        }

        //verify booking can be cancelled
        if(booking.getStatus() != BookingStatus.PENDING){
            throw new BookingCancellationException( "Only pending bookings can be cancelled");
        }

        //cancel the booking
        booking.setStatus(BookingStatus.CANCELLED);

        //make seats available --ShowSeats → AVAILABLE
        for (BookingItem bookingItem : booking.getBookingItems()) {

            ShowSeat showSeat = bookingItem.getShowSeat();

            showSeat.setStatus(ShowSeatStatus.AVAILABLE);


            // Release the Redis hold only after the database transaction commits.
            eventPublisher.publishEvent(
                    new SeatHoldReleaseEvent(
                            booking.getShow().getId(),
                            showSeat.getId(),
                            booking.getUser().getId()
                    )
            );

        }
        //Booking status and seat availability must change together.
        //If something fails halfway through, @Transactional should roll everything back.
        // thats why using @Transactional

        //save
        Booking savedBooking = bookingRepository.save(booking);

        return mapToResponse(savedBooking);

    }

}
