package Seatsure.Backend.controller;

import Seatsure.Backend.dto.CreateVenueRequestDto;
import Seatsure.Backend.dto.VenueResponseDto;
import Seatsure.Backend.service.VenueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/venues")
@RequiredArgsConstructor
public class VenueController {

    private final VenueService venueService;

    @PostMapping
    public ResponseEntity<VenueResponseDto> createVenues
            (@Valid @RequestBody CreateVenueRequestDto requestDto){

        VenueResponseDto response = venueService.createVenue(requestDto);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response);
    }


    @GetMapping("/{id}")
    public ResponseEntity<VenueResponseDto> getVenuesById(@PathVariable Long id){

        VenueResponseDto response = venueService.getVenuesById(id);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<VenueResponseDto>>getVenues(){

        List<VenueResponseDto> response = venueService.getVenues();

        return ResponseEntity.ok(response);
    }
}
