package com.moviebooking.modules.cinema.dto.response;

public record SeatTypeResponse(
        String code, String name, int capacity, int colSpan, long surcharge, String color, int sortOrder) {}
