package Seatsure.Backend.dto;


import Seatsure.Backend.enums.Category;
import Seatsure.Backend.enums.Status;
import lombok.*;

import java.time.LocalDateTime;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EventResponseDto {

    private Long id;

    private String name;

    private String description;

    private Category category;

    private Integer durationMinutes;

    private Status status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;


}
