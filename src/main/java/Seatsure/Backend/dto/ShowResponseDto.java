package Seatsure.Backend.dto;

import Seatsure.Backend.enums.Status;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ShowResponseDto {

    private Long id;

    private Long eventId;

    private Long venueId;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Status status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
