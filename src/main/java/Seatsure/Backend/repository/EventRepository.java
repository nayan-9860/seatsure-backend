package Seatsure.Backend.repository;

import Seatsure.Backend.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event , Long> {

}
