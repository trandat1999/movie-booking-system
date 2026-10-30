package com.moviebooking.modules.group.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;

/**
 * @param cinemaId null = rạp nào cũng được
 * @param rsvpDeadline hạn chốt: quá hạn mà chưa chốt suất thì nhóm hết hạn
 */
public record CreateGroupRequest(
        @NotBlank @Size(max = 120) String name,
        @NotNull Long movieId,
        Long cinemaId,
        LocalDate dateFrom,
        LocalDate dateTo,
        @NotNull @Future Instant rsvpDeadline) {}
