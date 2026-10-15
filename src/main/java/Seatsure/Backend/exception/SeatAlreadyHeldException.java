package Seatsure.Backend.exception;

public class SeatAlreadyHeldException extends RuntimeException{

    public SeatAlreadyHeldException(String message) {
        super(message);
    }
}
