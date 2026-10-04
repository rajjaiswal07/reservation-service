package com.pytm.reservationservice.service;

import com.pytm.reservationservice.dto.CreateEventRequest;
import com.pytm.reservationservice.dto.CreateSeatsRequest;
import com.pytm.reservationservice.dto.SeatResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EventService {

    private final JdbcTemplate jdbcTemplate;

    public EventService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long createEvent(CreateEventRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            throw new IllegalArgumentException("Event name is required");
        }

        return jdbcTemplate.queryForObject(
                """
                INSERT INTO events (name)
                VALUES (?)
                RETURNING id
                """,
                Long.class,
                request.name().trim()
        );
    }

    @Transactional
    public void createSeats(Long eventId, CreateSeatsRequest request) {
        if (request.seatNumbers() == null || request.seatNumbers().isEmpty()) {
            throw new IllegalArgumentException("At least one seat is required");
        }

        Integer eventCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM events WHERE id = ?",
                Integer.class,
                eventId
        );

        if (eventCount == null || eventCount == 0) {
            throw new IllegalArgumentException("Event not found");
        }

        for (String seatNumber : request.seatNumbers()) {
            if (seatNumber == null || seatNumber.isBlank()) {
                throw new IllegalArgumentException("Seat number cannot be blank");
            }

            jdbcTemplate.update(
                    """
                    INSERT INTO seats (event_id, seat_number, status)
                    VALUES (?, ?, 'AVAILABLE')
                    """,
                    eventId,
                    seatNumber.trim()
            );
        }
    }

    public List<SeatResponse> getSeats(Long eventId, boolean availableOnly) {
        String sql = """
                SELECT id, event_id, seat_number, status
                FROM seats
                WHERE event_id = ?
                """ + (availableOnly ? " AND status = 'AVAILABLE'" : "")
                + " ORDER BY seat_number";

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new SeatResponse(
                        rs.getLong("id"),
                        rs.getLong("event_id"),
                        rs.getString("seat_number"),
                        rs.getString("status")
                ),
                eventId
        );
    }
}