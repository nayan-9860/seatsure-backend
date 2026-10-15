package Seatsure.Backend.exception;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ApiErrorDto {

    private LocalDateTime timeStamp;
    private int status;
    private String error;
    private String message;
    private String path;
}
