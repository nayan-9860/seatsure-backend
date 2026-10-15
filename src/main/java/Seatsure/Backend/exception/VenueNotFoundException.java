package Seatsure.Backend.exception;

public class VenueNotFoundException extends RuntimeException{

    public VenueNotFoundException(String message){
        super(message);
    }
}
