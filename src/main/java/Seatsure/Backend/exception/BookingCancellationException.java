package Seatsure.Backend.exception;

public class BookingCancellationException extends RuntimeException{

    public BookingCancellationException(String message) {
        super(message);
    }
}
