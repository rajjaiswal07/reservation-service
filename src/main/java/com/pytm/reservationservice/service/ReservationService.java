package com.pytm.reservationservice.service;

import com.pytm.reservationservice.dto.CreateReservationRequest;
import com.pytm.reservationservice.dto.ReservationResponse;
import com.pytm.reservationservice.exception.ApiException;
import com.pytm.reservationservice.metrics.ReservationMetrics;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class ReservationService {

    private final JdbcTemplate jdbcTemplate;
    private final int maxReservationsPerUser;
    private final ReservationMetrics metrics;

    public ReservationService(
            JdbcTemplate jdbcTemplate,
            ReservationMetrics metrics,
            @Value("${reservation.max-per-user:5}") int maxReservationsPerUser) {

        this.jdbcTemplate = jdbcTemplate;
        this.metrics = metrics;
        this.maxReservationsPerUser = maxReservationsPerUser;
    }

    @Transactional
    public ReservationResponse reserveSeats(
            CreateReservationRequest request,
            String idempotencyKey) {

        validateRequest(request, idempotencyKey);

        List<Long> seatIds = request.seatIds().stream()
                .distinct()
                .sorted()
                .toList();

        if (seatIds.size() != request.seatIds().size()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Duplicate seat IDs are not allowed"
            );
        }

        String requestHash = generateRequestHash(
                request.eventId(), seatIds
        );

        /*
         * Serialize reservations for the same user.
         * The lock is held until the current transaction commits or rolls back.
         */
        jdbcTemplate.queryForObject(
                "SELECT pg_advisory_xact_lock(hashtextextended(?, 0))",
                Object.class,
                request.userId()
        );

        // Check idempotency before checking seat availability.
        List<ExistingReservation> existing = jdbcTemplate.query(
                """
                SELECT id, user_id, status, request_hash
                FROM reservations
                WHERE user_id = ? AND idempotency_key = ?
                """,
                (rs, rowNum) -> new ExistingReservation(
                        rs.getObject("id", UUID.class),
                        rs.getString("user_id"),
                        rs.getString("status"),
                        rs.getString("request_hash")
                ),
                request.userId(),
                idempotencyKey
        );

        if (!existing.isEmpty()) {
            ExistingReservation reservation = existing.get(0);

            if (!reservation.requestHash().equals(requestHash)) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "Idempotency key was already used with a different request"
                );
            }

            List<Long> originalSeatIds = jdbcTemplate.queryForList(
                    """
                    SELECT seat_id
                    FROM reservation_seats
                    WHERE reservation_id = ?
                    ORDER BY seat_id
                    """,
                    Long.class,
                    reservation.id()
            );

            metrics.idempotentReplay();

            return new ReservationResponse(
                    reservation.id(),
                    reservation.userId(),
                    reservation.status(),
                    originalSeatIds
            );
        }

        // Validate the event.
        Integer eventCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM events WHERE id = ?",
                Integer.class,
                request.eventId()
        );

        if (eventCount == null || eventCount == 0) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "Event not found"
            );
        }

        // Enforce the confirmed-reservation limit per user.
        Integer reservationCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM reservations
                WHERE user_id = ? AND status = 'CONFIRMED'
                """,
                Integer.class,
                request.userId()
        );

        if (reservationCount != null
                && reservationCount >= maxReservationsPerUser) {

            metrics.declinedPerUserLimit();

            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "User has reached the reservation limit"
            );
        }

        String placeholders = String.join(
                ",",
                seatIds.stream().map(id -> "?").toList()
        );

        // Lock all requested seats in a consistent order.
        List<LockedSeat> lockedSeats = jdbcTemplate.query(
                """
                SELECT id, event_id, status
                FROM seats
                WHERE id IN (%s)
                ORDER BY id
                FOR UPDATE
                """.formatted(placeholders),
                (rs, rowNum) -> new LockedSeat(
                        rs.getLong("id"),
                        rs.getLong("event_id"),
                        rs.getString("status")
                ),
                seatIds.toArray()
        );

        if (lockedSeats.size() != seatIds.size()) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "One or more seats were not found"
            );
        }

        if (lockedSeats.stream()
                .anyMatch(seat -> !seat.eventId().equals(request.eventId()))) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "One or more seats do not belong to this event"
            );
        }

        if (lockedSeats.stream()
                .anyMatch(seat -> !"AVAILABLE".equals(seat.status()))) {

            metrics.declinedSeatTaken();

            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "One or more seats are no longer available"
            );
        }

        UUID reservationId = UUID.randomUUID();

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

        for (Long seatId : seatIds) {
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
                """.formatted(placeholders),
                seatIds.toArray()
        );

        metrics.reservationConfirmed();

        return new ReservationResponse(
                reservationId,
                request.userId(),
                "CONFIRMED",
                seatIds
        );
    }

    private void validateRequest(
            CreateReservationRequest request,
            String idempotencyKey) {

        if (request == null
                || request.eventId() == null
                || request.eventId() <= 0
                || request.userId() == null
                || request.userId().isBlank()
                || request.seatIds() == null
                || request.seatIds().isEmpty()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Event ID, user ID, and at least one seat are required"
            );
        }

        if (request.seatIds().stream()
                .anyMatch(id -> id == null || id <= 0)) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Seat IDs must be positive"
            );
        }

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Idempotency-Key header is required"
            );
        }

        if (idempotencyKey.length() > 200) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Idempotency-Key must not exceed 200 characters"
            );
        }
    }

    private String generateRequestHash(Long eventId, List<Long> seatIds) {
        String canonicalRequest = eventId + ":" + seatIds;

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(
                    canonicalRequest.getBytes(StandardCharsets.UTF_8)
            );
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    private record LockedSeat(
            Long id,
            Long eventId,
            String status) {
    }

    private record ExistingReservation(
            UUID id,
            String userId,
            String status,
            String requestHash) {
    }

    public ReservationResponse getReservation(UUID reservationId) {

        List<ReservationResponse> results = jdbcTemplate.query(
                """
                SELECT id, user_id, status
                FROM reservations
                WHERE id = ?
                """,
                (rs, rowNum) -> {

                    UUID id = rs.getObject("id", UUID.class);
                    String userId = rs.getString("user_id");
                    String status = rs.getString("status");

                    List<Long> seatIds = jdbcTemplate.queryForList(
                            """
                            SELECT seat_id
                            FROM reservation_seats
                            WHERE reservation_id = ?
                            ORDER BY seat_id
                            """,
                            Long.class,
                            id
                    );

                    return new ReservationResponse(
                            id,
                            userId,
                            status,
                            seatIds
                    );
                },
                reservationId
        );

        if (results.isEmpty()) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "Reservation not found"
            );
        }

        return results.get(0);
    }
}