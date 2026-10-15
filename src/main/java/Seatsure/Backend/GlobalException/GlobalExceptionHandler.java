package Seatsure.Backend.GlobalException;

import Seatsure.Backend.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(VenueNotFoundException.class)
    public ResponseEntity<ApiErrorDto> handleVenueNotFound(
            VenueNotFoundException exception,
            HttpServletRequest request
    ){
        ApiErrorDto error = ApiErrorDto.builder()
                .timeStamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error(HttpStatus.NOT_FOUND.getReasonPhrase())
                .message(exception.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(error);

    }

    @ExceptionHandler(EventNotFoundException.class)
    public ResponseEntity<ApiErrorDto> handleEventNotFound(
            EventNotFoundException ex,
            HttpServletRequest request) {

        ApiErrorDto error = new ApiErrorDto(
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value(),
                "Event Not Found",
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(error);
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorDto> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ){
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));

        ApiErrorDto error = new ApiErrorDto(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Validation Failed",
                message,
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(error);
    }

    @ExceptionHandler(ShowNotFoundException.class)
    public ResponseEntity<ApiErrorDto> handleShowNotFoundException(
            ShowNotFoundException ex,
            HttpServletRequest request
    ){

        ApiErrorDto error = new ApiErrorDto(
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value(),
                "Show Not Found",
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorDto> handleIllegalArgumentException(
            IllegalArgumentException ex,
            HttpServletRequest request
    ) {

        ApiErrorDto error = new ApiErrorDto(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(error);
    }


    @ExceptionHandler(ShowScheduleConflictException.class)
    public ResponseEntity<ApiErrorDto> handleShowScheduleConflict(
            ShowScheduleConflictException ex,
            HttpServletRequest request) {

        ApiErrorDto error = new ApiErrorDto(
                LocalDateTime.now(),
                HttpStatus.CONFLICT.value(),
                "Show Schedule Conflict",
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(error);
    }

    @ExceptionHandler(SeatNotFoundException.class)
    public ResponseEntity<ApiErrorDto> handleSeatNotFoundException(
            SeatNotFoundException ex,
            HttpServletRequest request) {

        ApiErrorDto error = new ApiErrorDto(
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value(),
                "Seat Not Found",
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(error);
    }

    @ExceptionHandler(DuplicateSeatException.class)
    public ResponseEntity<ApiErrorDto> handleDuplicateSeatException(
            DuplicateSeatException ex,
            HttpServletRequest request) {

        ApiErrorDto error = new ApiErrorDto(
                LocalDateTime.now(),
                HttpStatus.CONFLICT.value(),
                "Duplicate Seat",
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(error);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiErrorDto> handleUserNotFoundException(
            UserNotFoundException ex,
            HttpServletRequest request) {

        ApiErrorDto error = new ApiErrorDto(
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value(),
                "User Not Found",
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(error);
    }

    @ExceptionHandler(ShowSeatNotFoundException.class)
    public ResponseEntity<ApiErrorDto> handleShowSeatNotFoundException(
            ShowSeatNotFoundException ex,
            HttpServletRequest request) {

        ApiErrorDto error = new ApiErrorDto(
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value(),
                "Show Seat Not Found",
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(error);
    }

    @ExceptionHandler(SeatNotAvailableException.class)
    public ResponseEntity<ApiErrorDto> handleSeatNotAvailableException(
            SeatNotAvailableException ex,
            HttpServletRequest request) {

        ApiErrorDto error = new ApiErrorDto(
                LocalDateTime.now(),
                HttpStatus.CONFLICT.value(),
                "Seat Not Available",
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(error);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiErrorDto> handleOptimisticLockingException(
            ObjectOptimisticLockingFailureException ex,
            HttpServletRequest request
    ){

        ApiErrorDto error = new ApiErrorDto(
                LocalDateTime.now(),
                HttpStatus.CONFLICT.value(),
                "Booking Conflict",
                "The selected seat was booked by another user. Please select another seat.",
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(error);
    }

    @ExceptionHandler(BookingNotFoundException.class)
    public ResponseEntity<ApiErrorDto> handleBookingNotFoundException(
            BookingNotFoundException ex,
            HttpServletRequest request) {

        ApiErrorDto error = new ApiErrorDto(
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value(),
                "Booking Not Found",
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(error);
    }

    @ExceptionHandler(BookingCancellationException.class)
    public ResponseEntity<ApiErrorDto> handleBookingCancellationException(
            BookingCancellationException ex,
            HttpServletRequest request) {

        ApiErrorDto error = new ApiErrorDto(
                LocalDateTime.now(),
                HttpStatus.CONFLICT.value(),
                "Booking Cancellation Conflict",
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(error);
    }

    @ExceptionHandler(SeatAlreadyHeldException.class)
    public ResponseEntity<ApiErrorDto> handleSeatAlreadyHeldException(
            SeatAlreadyHeldException ex,
            HttpServletRequest request) {

        ApiErrorDto error = new ApiErrorDto(
                LocalDateTime.now(),
                HttpStatus.CONFLICT.value(),
                "Seat Already Held",
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(error);
    }


    @ExceptionHandler(SeatHoldReleaseException.class)
    public ResponseEntity<ApiErrorDto> handleSeatHoldReleaseException(
            SeatHoldReleaseException ex,
            HttpServletRequest request) {

        ApiErrorDto error = new ApiErrorDto(
                LocalDateTime.now(),
                HttpStatus.CONFLICT.value(),
                "Seat Hold Conflict",
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(error);
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorDto> handleGenericException(
            Exception ex,
            HttpServletRequest request) {

        ApiErrorDto error = new ApiErrorDto(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error);
    }
}
