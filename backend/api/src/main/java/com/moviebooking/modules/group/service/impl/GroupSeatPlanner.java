package com.moviebooking.modules.group.service.impl;

import com.moviebooking.modules.group.dto.response.SeatSuggestionsResponse.SuggestionView;
import com.moviebooking.modules.setting.enums.SettingKey;
import com.moviebooking.modules.setting.service.SettingService;
import com.moviebooking.seatengine.ScoringWeights;
import com.moviebooking.seatengine.SeatLayout;
import com.moviebooking.seatengine.SeatSpec;
import com.moviebooking.seatengine.SeatSuggester;
import com.moviebooking.seatengine.Suggestion;
import com.moviebooking.seatengine.SuggestionRequest;
import com.moviebooking.seatengine.SuggestionResult;
import java.util.Collection;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Cầu nối tới thuật toán cụm ghế ({@code seat-engine}): dựng yêu cầu theo tham số hệ thống, đổi kết quả sang DTO. */
@Component
@RequiredArgsConstructor
class GroupSeatPlanner {

    private final SettingService settingService;
    private final SeatSuggester suggester = new SeatSuggester();

    SuggestionRequest request(int partySize, boolean allowSplit, Long maxPricePerPerson, Set<String> seatTypes) {
        return SuggestionRequest.of(partySize)
                .withAllowSplit(allowSplit)
                .withMaxRows(settingService.getInt(SettingKey.GROUP_SPLIT_MAX_ROWS))
                .withAllowedSeatTypes(seatTypes)
                .withMaxPricePerPerson(maxPricePerPerson)
                .withWeights(weights());
    }

    SuggestionResult suggest(SeatLayout layout, SuggestionRequest request) {
        return suggester.suggest(layout, request);
    }

    /** @throws IllegalArgumentException ghế không tồn tại/không trống hoặc không đủ chỗ */
    Suggestion evaluate(SeatLayout layout, SuggestionRequest request, Collection<Long> seatIds) {
        return suggester.evaluate(layout, request, seatIds);
    }

    /** Trọng số từ tham số hệ thống {@code seat_weight_*} (admin chỉnh được, PLAN.md 8.2). */
    ScoringWeights weights() {
        return new ScoringWeights(
                settingService.getInt(SettingKey.SEAT_WEIGHT_CENTER),
                settingService.getInt(SettingKey.SEAT_WEIGHT_ROWS),
                settingService.getInt(SettingKey.SEAT_WEIGHT_ALIGN),
                settingService.getInt(SettingKey.SEAT_WEIGHT_ORPHAN),
                settingService.getInt(SettingKey.SEAT_WEIGHT_PRICE),
                settingService.getInt(SettingKey.SEAT_WEIGHT_WASTE));
    }

    static SuggestionView view(int rank, Suggestion s) {
        return new SuggestionView(
                rank,
                s.seatIds(),
                s.labels(),
                s.rowCount(),
                s.capacity(),
                s.totalPrice(),
                s.score(),
                s.explanation(),
                s.breakdown());
    }

    /** Sơ đồ với một số ghế bị coi là đã có người (ghế vừa bị người khác giữ khi giữ ghế thất bại). */
    static SeatLayout withUnavailable(SeatLayout layout, Collection<Long> seatIds) {
        return new SeatLayout(
                layout.gridRows(),
                layout.gridCols(),
                layout.seats().stream()
                        .map(s -> seatIds.contains(s.id())
                                ? new SeatSpec(s.id(), s.rowLabel(), s.number(), s.gridRow(), s.gridCol(), s.colSpan(),
                                        s.capacity(), s.typeCode(), s.price(), false)
                                : s)
                        .toList());
    }

    static int availablePlaces(SeatLayout layout) {
        return layout.seats().stream()
                .filter(SeatSpec::available)
                .mapToInt(SeatSpec::capacity)
                .sum();
    }
}
