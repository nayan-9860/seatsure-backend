package Seatsure.Backend.service;

import Seatsure.Backend.dto.CreateShowRequestDto;
import Seatsure.Backend.dto.ShowResponseDto;
import Seatsure.Backend.entity.Event;
import Seatsure.Backend.entity.Show;
import Seatsure.Backend.entity.Venue;
import Seatsure.Backend.exception.EventNotFoundException;
import Seatsure.Backend.exception.ShowNotFoundException;
import Seatsure.Backend.exception.ShowScheduleConflictException;
import Seatsure.Backend.exception.VenueNotFoundException;
import Seatsure.Backend.repository.EventRepository;
import Seatsure.Backend.repository.ShowRepository;
import Seatsure.Backend.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ShowService {

    private final ShowRepository showRepository;
    private final EventRepository eventRepository;
    private final VenueRepository venueRepository;
    private final ModelMapper modelMapper;
    private final ShowSeatService showSeatService;

    @Transactional
    public ShowResponseDto createShow(CreateShowRequestDto requestDto){

        //validates times
        if(!requestDto.getEndTime().isAfter(requestDto.getStartTime())){
            throw new IllegalArgumentException("End Time must be after startTime");
        }

        //find Event by evetId
        Event event = eventRepository.findById(requestDto.getEventId())
                .orElseThrow(() -> new EventNotFoundException(
                        "Event was not found with id: " + requestDto.getEventId()));

        //find Venue
        Venue venue = venueRepository.findById(requestDto.getVenueId())
                .orElseThrow(() -> new VenueNotFoundException(
                        "venue is not found with id : " + requestDto.getVenueId()));

        //preventing overlapping
        //Two shows should not overlap in the same venue.

        boolean overlap = showRepository.existsOverlappingShow(
                requestDto.getVenueId(),
                requestDto.getStartTime(),
                requestDto.getEndTime()
        );

        if (overlap) {
            throw new ShowScheduleConflictException(
                    "Another show is already scheduled in this venue during the selected time"
            );
        }

        //create show
        //Show show = modelMapper.map(requestDto , Show.class);

//        show.setEvent(event);
//        show.setVenue(venue);
        Show show = Show.builder()
                .event(event)
                .venue(venue)
                .startTime(requestDto.getStartTime())
                .endTime(requestDto.getEndTime())
                .status(requestDto.getStatus())
                .build();

        Show savedShow = showRepository.save(show);

        //Create ShowSeats automatically
        showSeatService.initialiseShowSeats(
                savedShow,
                requestDto
        );

        return modelMapper.map(savedShow , ShowResponseDto.class);
    }

    public ShowResponseDto getShowById(Long id){

       Show show =  showRepository.findById(id)
               .orElseThrow(() -> new ShowNotFoundException("show is not found with id: " + id));

       return modelMapper.map(show , ShowResponseDto.class);
    }

    public List<ShowResponseDto> getAllShows(){

        return showRepository.findAll()
                .stream()
                .map(show -> modelMapper.map(show , ShowResponseDto.class))
                .toList();

    }
}
