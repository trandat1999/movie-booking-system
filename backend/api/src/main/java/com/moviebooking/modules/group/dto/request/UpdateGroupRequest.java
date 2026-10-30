package com.moviebooking.modules.group.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;

public record UpdateGroupRequest(
        @NotBlank @Size(max = 120) String name,
        Long cinemaId,
        LocalDate dateFrom,
        LocalDate dateTo,
        @NotNull Instant rsvpDeadline) {}
