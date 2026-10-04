package com.pytm.reservationservice.controller;

import com.pytm.reservationservice.dto.CreateReservationRequest;
import com.pytm.reservationservice.dto.ReservationResponse;
import com.pytm.reservationservice.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Reservations", description = "Seat reservation APIs")
@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @Operation(summary = "Create a reservation")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationResponse createReservation(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody CreateReservationRequest request) {

        return reservationService.reserveSeats(request, idempotencyKey);
    }

    @Operation(summary = "Get a reservation")
    @GetMapping("/{reservationId}")
    public ReservationResponse getReservation(
            @PathVariable UUID reservationId) {

        return reservationService.getReservation(reservationId);
    }
}