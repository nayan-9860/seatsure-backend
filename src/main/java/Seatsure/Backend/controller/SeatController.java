package Seatsure.Backend.controller;

import Seatsure.Backend.dto.CreateSeatRequestDto;
import Seatsure.Backend.dto.SeatResponseDto;
import Seatsure.Backend.service.SeatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/seats")
@RequiredArgsConstructor
public class SeatController {

    private final SeatService seatService;

    @PostMapping
    public ResponseEntity<SeatResponseDto> createSeat(@Valid
                                                      @RequestBody CreateSeatRequestDto request){

       SeatResponseDto response =  seatService.createSeat(request);

       return ResponseEntity.status(HttpStatus.CREATED)
               .body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SeatResponseDto> getSeatById(@PathVariable Long id){

        SeatResponseDto response = seatService.getSeatById(id);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<SeatResponseDto>> getAllSeats(){

        return ResponseEntity.ok(seatService.getAllSeats());
    }
}
