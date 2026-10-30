package com.moviebooking.modules.group.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * @param suggestionLogId lần đề xuất mà phương án đến từ đó (để thống kê), có thể null
 * @param chosenRank thứ hạng phương án (1..K), 0 = tự chọn tay
 */
public record GroupHoldRequest(
        @NotEmpty @Size(max = 50) List<@NotNull Long> seatIds, Long suggestionLogId, @Min(0) Integer chosenRank) {}
