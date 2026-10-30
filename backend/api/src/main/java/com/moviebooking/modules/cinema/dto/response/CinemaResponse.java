package com.moviebooking.modules.cinema.dto.response;

import com.moviebooking.modules.cinema.enums.CinemaStatus;
import java.util.List;

public record CinemaResponse(
        Long id,
        String name,
        String address,
        String city,
        String phone,
        String description,
        String imageUrl,
        List<String> amenities,
        CinemaStatus status,
        long roomCount) {}
