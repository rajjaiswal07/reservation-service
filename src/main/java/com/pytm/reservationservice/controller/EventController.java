package com.pytm.reservationservice.controller;

import com.pytm.reservationservice.dto.CreateEventRequest;
import com.pytm.reservationservice.dto.CreateSeatsRequest;
import com.pytm.reservationservice.dto.SeatResponse;
import com.pytm.reservationservice.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Events", description = "Event and seat management APIs")
@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @Operation(summary = "Create an event")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Long> createEvent(
            @RequestBody CreateEventRequest request) {
        Long eventId = eventService.createEvent(request);
        return Map.of("eventId", eventId);
    }

    @Operation(summary = "Create seats for an event")
    @PostMapping("/{eventId}/seats")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> createSeats(
            @PathVariable Long eventId,
            @RequestBody CreateSeatsRequest request) {
        eventService.createSeats(eventId, request);
        return Map.of("message", "Seats created successfully");
    }

    @Operation(summary = "Get seats for an event")
    @GetMapping("/{eventId}/seats")
    public List<SeatResponse> getSeats(
            @PathVariable Long eventId,
            @RequestParam(defaultValue = "false") boolean availableOnly) {
        return eventService.getSeats(eventId, availableOnly);
    }
}