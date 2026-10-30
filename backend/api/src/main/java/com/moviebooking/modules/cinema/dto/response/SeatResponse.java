package com.moviebooking.modules.cinema.dto.response;

public record SeatResponse(
        Long id,
        String rowLabel,
        int number,
        int gridRow,
        int gridCol,
        int colSpan,
        String seatTypeCode,
        boolean active) {}
