package Seatsure.Backend.controller;


import Seatsure.Backend.dto.BookingResponseDto;
import Seatsure.Backend.dto.CreateBookingRequestDto;
import Seatsure.Backend.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<BookingResponseDto> createBooking(
            @Valid @RequestBody CreateBookingRequestDto request,
            Authentication authentication) {

        String email = authentication.getName();

        BookingResponseDto response =
                bookingService.createBooking(
                        email,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<BookingResponseDto>> getMyBookings(
            Authentication authentication
    ) {

        String email = authentication.getName();

        List<BookingResponseDto> bookings =
                bookingService.getMyBookings(email);

        return ResponseEntity.ok(bookings);
    }

    @PatchMapping("/{bookingId}/cancel")
    public ResponseEntity<BookingResponseDto> cancelBookings(@PathVariable Long bookingId ,
                                                             Authentication authentication){

        String email = authentication.getName();

        BookingResponseDto response =
                bookingService.cancelBooking(email, bookingId);

        return ResponseEntity.ok(response);

    }
}
