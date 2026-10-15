package Seatsure.Backend.service;


import Seatsure.Backend.dto.CreateEventRequestDto;
import Seatsure.Backend.dto.EventResponseDto;
import Seatsure.Backend.entity.Event;
import Seatsure.Backend.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final ModelMapper modelMapper;

    public EventResponseDto createEvent(CreateEventRequestDto requestDto){

        Event event = modelMapper.map(requestDto , Event.class);

        Event savedEvent = eventRepository.save(event);

        return modelMapper.map(savedEvent , EventResponseDto.class);
    }

    public EventResponseDto getEventById(Long id){

        Event event = eventRepository.findById(id)
                .orElseThrow(() ->new RuntimeException("event not found with id : " + id));

        return modelMapper.map(event , EventResponseDto.class);
    }

    public List<EventResponseDto> getALLEvents(){

        return eventRepository.findAll()
                .stream()
                .map(event -> modelMapper.map(event , EventResponseDto.class))
                .toList();
    }
}
