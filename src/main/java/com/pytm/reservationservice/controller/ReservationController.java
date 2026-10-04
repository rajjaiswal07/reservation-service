package com.pytm.reservationservice.controller;

import com.pytm.reservationservice.dto.CreateReservationRequest;
import com.pytm.reservationservice.dto.ReservationResponse;
import com.pytm.reservationservice.service.ReservationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationResponse createReservation(
            @RequestBody CreateReservationRequest request) {
        return reservationService.reserveSeats(request);
    }
}