package com.pytm.reservationservice.dto;

import java.util.List;
import java.util.UUID;

public record ReservationResponse(
        UUID reservationId,
        String userId,
        String status,
        List<Long> seatIds
) {
}