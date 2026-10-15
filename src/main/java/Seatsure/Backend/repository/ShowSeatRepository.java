package Seatsure.Backend.repository;

import Seatsure.Backend.entity.ShowSeat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShowSeatRepository extends JpaRepository<ShowSeat , Long> {

    List<ShowSeat> findByShowId(Long showId);

    List<ShowSeat> findByShowIdAndIdIn(
            Long showId,
            List<Long> showSeatIds
    );
}
