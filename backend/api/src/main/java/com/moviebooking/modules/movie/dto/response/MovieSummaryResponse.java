package com.moviebooking.modules.movie.dto.response;

import com.moviebooking.modules.movie.enums.AgeRating;
import com.moviebooking.modules.movie.enums.MovieStatus;
import java.time.LocalDate;
import java.util.List;

public record MovieSummaryResponse(
        Long id,
        String title,
        String originalTitle,
        String slug,
        int durationMin,
        AgeRating ageRating,
        String language,
        LocalDate releaseDate,
        String posterUrl,
        MovieStatus status,
        List<GenreResponse> genres) {}
