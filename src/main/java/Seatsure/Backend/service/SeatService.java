package Seatsure.Backend.service;

import Seatsure.Backend.dto.CreateSeatRequestDto;
import Seatsure.Backend.dto.SeatResponseDto;
import Seatsure.Backend.entity.Seat;
import Seatsure.Backend.entity.Venue;
import Seatsure.Backend.exception.DuplicateSeatException;
import Seatsure.Backend.exception.SeatNotFoundException;
import Seatsure.Backend.exception.VenueNotFoundException;
import Seatsure.Backend.repository.SeatRepository;
import Seatsure.Backend.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SeatService {

    private final SeatRepository seatRepository;
    private final VenueRepository venueRepository;
    private final ModelMapper modelMapper;

    public SeatResponseDto createSeat(CreateSeatRequestDto request){

        //A seat belongs to a specific venue, and the venue must exist before we create the seat.
        //find venue
        Venue venue = venueRepository.findById(request.getVenueId())
                .orElseThrow(() -> new VenueNotFoundException(
                        "venue is not found with id : " + request.getVenueId()));

        boolean exists = seatRepository.existsByVenueIdAndSeatNumber(
                request.getVenueId(),
                request.getSeatNumber()
        );

        if (exists) {
            throw new DuplicateSeatException(
                    "Seat " + request.getSeatNumber()
                            + " already exists in this venue"
            );
        }

        //2. DTO → Entity
        //Seat seat = modelMapper.map(request , Seat.class);

        Seat seat = Seat.builder()
                .venue(venue)
                .seatNumber(request.getSeatNumber())
                .rowNumber(request.getRowNumber())
                .seatType(request.getSeatType())
                .build();

        // 3. Set relationship
        seat.setVenue(venue);

        //save seat in db
        Seat savedSeat = seatRepository.save(seat);

        //entity -> dto
        return modelMapper.map(savedSeat, SeatResponseDto.class);
    }

    public SeatResponseDto getSeatById(Long id){

        Seat seat = seatRepository.findById(id)
                .orElseThrow(() -> new SeatNotFoundException("seat is not found with id : " + id));

        return modelMapper.map(seat , SeatResponseDto.class);
    }

    public List<SeatResponseDto> getAllSeats(){

        return seatRepository.findAll()
                .stream()
                .map(seat -> modelMapper.map(seat, SeatResponseDto.class))
                .toList();
    }
}
