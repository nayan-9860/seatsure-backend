package Seatsure.Backend.controller;

import Seatsure.Backend.dto.LoginRequestDto;
import Seatsure.Backend.dto.LoginResponseDto;
import Seatsure.Backend.dto.RegisterRequestDto;
import Seatsure.Backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid
                                           @RequestBody RegisterRequestDto requestDto){
        authService.register(requestDto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body("user is successfully registered");


    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@Valid
                                                  @RequestBody LoginRequestDto requestDto){

        String token = authService.login(requestDto);

        LoginResponseDto response = LoginResponseDto.builder()
                .token(token)
                .build();

        return ResponseEntity.ok(response);
    }
}
