package Seatsure.Backend.controller;

import Seatsure.Backend.dto.CreatePaymentRequestDto;
import Seatsure.Backend.dto.PaymentResponseDto;
import Seatsure.Backend.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentResponseDto> createPayment(@Valid
                                                            @RequestBody CreatePaymentRequestDto requestDto ,
                                                            Authentication authentication){

        String email = authentication.getName();

        PaymentResponseDto response =
                paymentService.createPayment(email, requestDto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }


    @PostMapping("/{paymentId}/success")
    public ResponseEntity<PaymentResponseDto> successPayment(
            @PathVariable Long paymentId,
            Authentication authentication) {

        String email = authentication.getName();

        PaymentResponseDto response =
                paymentService.successPayment(email, paymentId);

        return ResponseEntity.ok(response);
    }


    @PostMapping("/{paymentId}/failure")
    public ResponseEntity<PaymentResponseDto> failurePayment(
            @PathVariable Long paymentId,
            Authentication authentication) {

        String email = authentication.getName();

        PaymentResponseDto response =
                paymentService.failurePayment(email, paymentId);

        return ResponseEntity.ok(response);

    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponseDto> getPayment(
            @PathVariable Long paymentId,
            Authentication authentication) {

        String email = authentication.getName();

        PaymentResponseDto response =
                paymentService.getPayment(email, paymentId);

        return ResponseEntity.ok(response);
    }
}

