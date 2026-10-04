package com.pytm.reservationservice.controller;

import com.pytm.reservationservice.dto.CreateEventRequest;
import com.pytm.reservationservice.dto.CreateSeatsRequest;
import com.pytm.reservationservice.dto.SeatResponse;
import com.pytm.reservationservice.service.EventService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Long> createEvent(
            @RequestBody CreateEventRequest request) {
        Long eventId = eventService.createEvent(request);
        return Map.of("eventId", eventId);
    }

    @PostMapping("/{eventId}/seats")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> createSeats(
            @PathVariable Long eventId,
            @RequestBody CreateSeatsRequest request) {
        eventService.createSeats(eventId, request);
        return Map.of("message", "Seats created successfully");
    }

    @GetMapping("/{eventId}/seats")
    public List<SeatResponse> getSeats(
            @PathVariable Long eventId,
            @RequestParam(defaultValue = "false") boolean availableOnly) {
        return eventService.getSeats(eventId, availableOnly);
    }
}