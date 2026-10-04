package com.pytm.reservationservice.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ReservationMetrics {

    private final Counter confirmedReservations;
    private final Counter declinedSeatTaken;
    private final Counter declinedPerUserLimit;
    private final Counter declinedIdempotentReplay;

    public ReservationMetrics(
            MeterRegistry meterRegistry,
            JdbcTemplate jdbcTemplate) {

        confirmedReservations = Counter.builder("reservation.confirmed")
                .description("Number of confirmed reservations")
                .register(meterRegistry);

        declinedSeatTaken = Counter.builder("reservation.declined")
                .tag("reason", "seat-taken")
                .description("Reservations declined because seats were unavailable")
                .register(meterRegistry);

        declinedPerUserLimit = Counter.builder("reservation.declined")
                .tag("reason", "per-user-limit")
                .description("Reservations declined because the user reached the limit")
                .register(meterRegistry);

        declinedIdempotentReplay = Counter.builder("reservation.declined")
                .tag("reason", "idempotent-replay")
                .description("Idempotent reservation replays")
                .register(meterRegistry);

        Gauge.builder(
                        "reservation.seats.available",
                        jdbcTemplate,
                        template -> {
                            Integer count = template.queryForObject(
                                    "SELECT COUNT(*) FROM seats WHERE status = 'AVAILABLE'",
                                    Integer.class
                            );
                            return count == null ? 0 : count;
                        })
                .description("Number of currently available seats")
                .register(meterRegistry);
    }

    public void reservationConfirmed() {
        confirmedReservations.increment();
    }

    public void declinedSeatTaken() {
        declinedSeatTaken.increment();
    }

    public void declinedPerUserLimit() {
        declinedPerUserLimit.increment();
    }

    public void idempotentReplay() {
        declinedIdempotentReplay.increment();
    }
}