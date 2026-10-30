package com.moviebooking.modules.group.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Trưởng nhóm sửa phân phối vé (GRP17). */
public record AssignmentRequest(@NotEmpty @Valid List<Item> items) {

    /** @param guestLabel tên khách đi kèm của thành viên {@code userId}; null = vé của chính thành viên */
    public record Item(@NotBlank String ticketCode, @NotNull Long userId, @Size(max = 100) String guestLabel) {}
}
