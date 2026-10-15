package Seatsure.Backend.controller;

import Seatsure.Backend.dto.CreateEventRequestDto;
import Seatsure.Backend.dto.EventResponseDto;
import Seatsure.Backend.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @PostMapping
    public ResponseEntity<EventResponseDto> createEvents(
            @Valid @RequestBody CreateEventRequestDto requestDto){

        EventResponseDto response = eventService.createEvent(requestDto);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventResponseDto> getEventById(
            @PathVariable Long id){

        EventResponseDto response = eventService.getEventById(id);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<EventResponseDto>> getAllEvents(){

        List<EventResponseDto> response = eventService.getALLEvents();

        return ResponseEntity.ok(response);
    }


    @GetMapping("/admin-test")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> adminTest() {
        return ResponseEntity.ok("Admin access granted");
    }

}
