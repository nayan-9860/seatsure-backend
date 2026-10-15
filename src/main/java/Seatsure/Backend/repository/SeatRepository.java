package Seatsure.Backend.repository;

import Seatsure.Backend.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SeatRepository extends JpaRepository<Seat , Long> {

    @Query("""
        SELECT COUNT(s) > 0
        FROM Seat s
        WHERE s.venue.id = :venueId
        AND s.seatNumber = :seatNumber
        """)
    boolean existsByVenueIdAndSeatNumber(
            @Param("venueId") Long venueId,
            @Param("seatNumber") String seatNumber
    );

    List<Seat> findByVenueId(Long venueId);
}
