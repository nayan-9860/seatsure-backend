package Seatsure.Backend.repository;

import Seatsure.Backend.entity.Show;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface ShowRepository extends JpaRepository<Show , Long> {

    @Query("""
        SELECT COUNT(s) > 0
        FROM Show s
        WHERE s.venue.id = :venueId
        AND s.startTime < :endTime
        AND s.endTime > :startTime
        """)
    boolean existsOverlappingShow(
            @Param("venueId")Long venueId,
            @Param("startTime")LocalDateTime startTime,
            @Param("endTime")LocalDateTime endTime
            );
}
