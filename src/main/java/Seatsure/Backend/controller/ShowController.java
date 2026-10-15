package Seatsure.Backend.controller;

import Seatsure.Backend.dto.CreateShowRequestDto;
import Seatsure.Backend.dto.ShowResponseDto;
import Seatsure.Backend.entity.Show;
import Seatsure.Backend.service.ShowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shows")
@RequiredArgsConstructor
public class ShowController {

    private final ShowService showService;

    @PostMapping
    public ResponseEntity<ShowResponseDto> createShows(@Valid
                                                       @RequestBody CreateShowRequestDto requestDto){

        ShowResponseDto response = showService.createShow(requestDto);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShowResponseDto> getShowById(@PathVariable Long id){

        ShowResponseDto response = showService.getShowById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<ShowResponseDto> >getAllShows(){

        return ResponseEntity.ok(showService.getAllShows());
    }
}
