package com.pytm.reservationservice.service;

import com.pytm.reservationservice.dto.CreateReservationRequest;
import com.pytm.reservationservice.dto.ReservationResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ReservationService {

    private final JdbcTemplate jdbcTemplate;

    public ReservationService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public ReservationResponse reserveSeats(CreateReservationRequest request) {
        validateRequest(request);

        List<Long> requestedSeatIds = request.seatIds()
                .stream()
                .distinct()
                .sorted()
                .toList();

        if (requestedSeatIds.size() != request.seatIds().size()) {
            throw new IllegalArgumentException("Duplicate seat IDs are not allowed");
        }

        // Lock seats in ascending ID order to reduce deadlock risk.
        List<LockedSeat> lockedSeats = jdbcTemplate.query(
                """
                SELECT id, event_id, status
                FROM seats
                WHERE id IN (%s)
                ORDER BY id
                FOR UPDATE
                """.formatted(
                        String.join(",", requestedSeatIds.stream()
                                .map(id -> "?")
                                .toList())
                ),
                (rs, rowNum) -> new LockedSeat(
                        rs.getLong("id"),
                        rs.getLong("event_id"),
                        rs.getString("status")
                ),
                requestedSeatIds.toArray()
        );

        if (lockedSeats.size() != requestedSeatIds.size()) {
            throw new IllegalArgumentException("One or more seats do not exist");
        }

        boolean wrongEvent = lockedSeats.stream()
                .anyMatch(seat -> !seat.eventId().equals(request.eventId()));

        if (wrongEvent) {
            throw new IllegalArgumentException(
                    "One or more seats do not belong to this event"
            );
        }

        boolean unavailable = lockedSeats.stream()
                .anyMatch(seat -> !"AVAILABLE".equals(seat.status()));

        if (unavailable) {
            throw new IllegalStateException(
                    "One or more seats are no longer available"
            );
        }

        UUID reservationId = UUID.randomUUID();

        // The existing schema requires these idempotency columns.
        // Full retry and key/body mismatch handling comes in a later step.
        String idempotencyKey = UUID.randomUUID().toString();
        String requestHash = UUID.randomUUID().toString();

        jdbcTemplate.update(
                """
                INSERT INTO reservations (
                    id, user_id, status, idempotency_key, request_hash
                )
                VALUES (?, ?, 'CONFIRMED', ?, ?)
                """,
                reservationId,
                request.userId(),
                idempotencyKey,
                requestHash
        );

        for (Long seatId : requestedSeatIds) {
            jdbcTemplate.update(
                    """
                    INSERT INTO reservation_seats (reservation_id, seat_id)
                    VALUES (?, ?)
                    """,
                    reservationId,
                    seatId
            );
        }

        jdbcTemplate.update(
                """
                UPDATE seats
                SET status = 'RESERVED'
                WHERE id IN (%s)
                """.formatted(
                        String.join(",", requestedSeatIds.stream()
                                .map(id -> "?")
                                .toList())
                ),
                requestedSeatIds.toArray()
        );

        return new ReservationResponse(
                reservationId,
                request.userId(),
                "CONFIRMED",
                requestedSeatIds
        );
    }

    private void validateRequest(CreateReservationRequest request) {
        if (request == null
                || request.eventId() == null
                || request.userId() == null
                || request.userId().isBlank()
                || request.seatIds() == null
                || request.seatIds().isEmpty()) {
            throw new IllegalArgumentException(
                    "Event ID, user ID, and at least one seat ID are required"
            );
        }

        if (request.seatIds().stream().anyMatch(id -> id == null || id <= 0)) {
            throw new IllegalArgumentException("Seat IDs must be positive");
        }
    }

    private record LockedSeat(Long id, Long eventId, String status) {
    }
}