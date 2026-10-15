package Seatsure.Backend.service;

import Seatsure.Backend.dto.CreateVenueRequestDto;
import Seatsure.Backend.dto.VenueResponseDto;
import Seatsure.Backend.entity.Venue;
import Seatsure.Backend.exception.VenueNotFoundException;
import Seatsure.Backend.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VenueService {

    private final VenueRepository venueRepository;
    private final ModelMapper modelMapper;

    public VenueResponseDto createVenue(CreateVenueRequestDto requestDto){

        //covert the requestDto into venue
        Venue venue = modelMapper.map(requestDto , Venue.class);

        //then save the data in venue
        Venue savedVenue = venueRepository.save(venue);

        //again return data of venue to response
        return  modelMapper.map(savedVenue , VenueResponseDto.class);
    }


    public VenueResponseDto getVenuesById(Long id) {

        Venue venue = venueRepository.findById(id)
                .orElseThrow(() -> new VenueNotFoundException("venue is not found" + id));

        return modelMapper.map(venue , VenueResponseDto.class);
    }

    public List<VenueResponseDto> getVenues(){

        List<Venue> venue = venueRepository.findAll();

        return venue.stream()
                .map(venue1 -> modelMapper.map(venue1 , VenueResponseDto.class) )
                .toList();
    }

}
