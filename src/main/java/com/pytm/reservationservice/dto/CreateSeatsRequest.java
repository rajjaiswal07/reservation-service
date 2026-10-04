package com.pytm.reservationservice.dto;

import java.util.List;

public record CreateSeatsRequest(List<String> seatNumbers) {
}