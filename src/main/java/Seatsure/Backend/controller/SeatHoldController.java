package Seatsure.Backend.controller;

import Seatsure.Backend.dto.CreateSeatHoldRequestDto;
import Seatsure.Backend.service.SeatHoldService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/seat-holds")
@RequiredArgsConstructor
public class SeatHoldController {

    private final SeatHoldService seatHoldService;

    @PostMapping
    public ResponseEntity<String> holdSeat(
            @Valid @RequestBody CreateSeatHoldRequestDto request,
            Authentication authentication) {

        seatHoldService.holdSeat(
                request.getShowId(),
                request.getShowSeatId(),
                authentication.getName()
        );

        return ResponseEntity.ok(
                "Seat held successfully for 5 minutes"
        );
    }

    @DeleteMapping
    public ResponseEntity<String> releaseSeat(
            @Valid @RequestBody CreateSeatHoldRequestDto request,
            Authentication authentication) {

        seatHoldService.releaseSeat(
                request.getShowId(),
                request.getShowSeatId(),
                authentication.getName()
        );

        return ResponseEntity.ok(
                "Seat hold released successfully"
        );
    }
}
