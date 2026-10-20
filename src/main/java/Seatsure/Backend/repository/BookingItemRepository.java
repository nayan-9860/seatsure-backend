package Seatsure.Backend.repository;

import Seatsure.Backend.entity.BookingItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingItemRepository extends JpaRepository<BookingItem , Long> {

    List<BookingItem> findByShowSeatId(Long showSeatId);

}
