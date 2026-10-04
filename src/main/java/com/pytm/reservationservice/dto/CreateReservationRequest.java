package com.pytm.reservationservice.dto;

import java.util.List;

public record CreateReservationRequest(
        Long eventId,
        String userId,
        List<Long> seatIds
) {
}