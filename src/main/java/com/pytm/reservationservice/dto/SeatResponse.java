package com.pytm.reservationservice.dto;

public record SeatResponse(
        Long id,
        Long eventId,
        String seatNumber,
        String status
) {
}