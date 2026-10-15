
package Seatsure.Backend;

import Seatsure.Backend.entity.Event;
import Seatsure.Backend.entity.Seat;
import Seatsure.Backend.entity.Show;
import Seatsure.Backend.entity.ShowSeat;
import Seatsure.Backend.entity.Venue;
import Seatsure.Backend.enums.Category;
import Seatsure.Backend.enums.SeatType;
import Seatsure.Backend.enums.ShowSeatStatus;
import Seatsure.Backend.enums.Status;
import Seatsure.Backend.repository.EventRepository;
import Seatsure.Backend.repository.SeatRepository;
import Seatsure.Backend.repository.ShowRepository;
import Seatsure.Backend.repository.ShowSeatRepository;
import Seatsure.Backend.repository.VenueRepository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ShowSeatOptimisticLockingTest {

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
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void staleVersionUpdateShouldFail() {

        TransactionTemplate tx =
                new TransactionTemplate(transactionManager);

        // Step 1: Create the test data.
        Long showSeatId = tx.execute(status -> {

            Venue venue = venueRepository.save(
                    Venue.builder()
                            .name("Test Venue")
                            .city("Mumbai")
                            .address("Test Address")
                            .capacity(100)
                            .build()
            );

            Event event = eventRepository.save(
                    Event.builder()
                            .name("Test Event")
                            .description("Event for optimistic locking test")
                            .category(Category.values()[0])
                            .durationMinutes(120)
                            .status(Status.values()[0])
                            .build()
            );

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

            Seat seat = seatRepository.save(
                    Seat.builder()
                            .venue(venue)
                            .seatNumber("TEST-A1")
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

            showSeatRepository.flush();

            return showSeat.getId();
        });

        assertNotNull(showSeatId);

        // Step 2: Read the ShowSeat and keep its old version.
        ShowSeat staleSeat = tx.execute(status -> {

            ShowSeat seat = showSeatRepository
                    .findById(showSeatId)
                    .orElseThrow();

            entityManager.detach(seat);

            return seat;
        });

        assertNotNull(staleSeat);

        // Step 3: Another transaction updates the same seat.
        tx.executeWithoutResult(status -> {

            ShowSeat currentSeat = showSeatRepository
                    .findById(showSeatId)
                    .orElseThrow();

            currentSeat.setStatus(ShowSeatStatus.HELD);

            entityManager.flush();
        });

        // Step 4: Try to update using the old version.
        assertThrows(
                ObjectOptimisticLockingFailureException.class,
                () -> tx.executeWithoutResult(status -> {

                    staleSeat.setStatus(ShowSeatStatus.BOOKED);

                    showSeatRepository.save(staleSeat);

                    entityManager.flush();
                })
        );
    }
}