package com.moviebooking.modules.group.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** @param ticketCount null = theo tổng hợp (thành viên "Có" + khách đi kèm) */
public record LockGroupRequest(@NotNull Long showtimeId, @Positive Integer ticketCount) {}
