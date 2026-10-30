package com.moviebooking.modules.movie.dto.response;

import com.moviebooking.modules.movie.enums.AgeRating;
import com.moviebooking.modules.movie.enums.MovieStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record MovieDetailResponse(
        Long id,
        String title,
        String originalTitle,
        String slug,
        String description,
        int durationMin,
        AgeRating ageRating,
        String language,
        LocalDate releaseDate,
        String posterUrl,
        String bannerUrl,
        String trailerUrl,
        MovieStatus status,
        List<GenreResponse> genres,
        List<PersonResponse> directors,
        List<PersonResponse> actors,
        Instant createdAt,
        Instant updatedAt) {}
