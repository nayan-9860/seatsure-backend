package Seatsure.Backend.controller;

import Seatsure.Backend.dto.ShowResponseDto;
import Seatsure.Backend.dto.ShowSeatResponseDto;
import Seatsure.Backend.service.ShowSeatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/shows")
@RequiredArgsConstructor
public class ShowSeatController {

    private final ShowSeatService showSeatService;

    @GetMapping("/{showId}/seats")
    public ResponseEntity<List<ShowSeatResponseDto>> getShowSeats(@PathVariable Long showId){

        List<ShowSeatResponseDto> response = showSeatService.getShowSeats(showId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response);
    }


}
