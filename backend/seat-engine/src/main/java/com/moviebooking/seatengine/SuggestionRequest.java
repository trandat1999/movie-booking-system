package com.moviebooking.seatengine;

import java.util.Set;

/**
 * Yêu cầu đề xuất cụm ghế.
 *
 * @param partySize số người cần chỗ ngồi (n)
 * @param allowSplit cho phép chia nhóm ngồi nhiều hàng liên tiếp
 * @param maxRows số hàng tối đa khi chia
 * @param allowedSeatTypes chỉ dùng các loại ghế này; rỗng = mọi loại
 * @param maxPricePerPerson ngân sách mỗi người (VND, ghế đôi tính giá / 2); null = không giới hạn. Là tiêu chí mềm:
 *     vượt ngân sách bị trừ điểm chứ không bị loại
 * @param topK số phương án tối đa trả về
 */
public record SuggestionRequest(
        int partySize,
        boolean allowSplit,
        int maxRows,
        Set<String> allowedSeatTypes,
        Long maxPricePerPerson,
        int topK,
        ScoringWeights weights) {

    public static final int DEFAULT_MAX_ROWS = 3;
    public static final int DEFAULT_TOP_K = 5;

    public SuggestionRequest {
        if (partySize < 1) {
            throw new IllegalArgumentException("partySize must be >= 1");
        }
        if (maxRows < 1 || topK < 1) {
            throw new IllegalArgumentException("maxRows and topK must be >= 1");
        }
        if (maxPricePerPerson != null && maxPricePerPerson <= 0) {
            throw new IllegalArgumentException("maxPricePerPerson must be positive");
        }
        allowedSeatTypes = allowedSeatTypes == null ? Set.of() : Set.copyOf(allowedSeatTypes);
        weights = weights == null ? ScoringWeights.DEFAULT : weights;
    }

    /** Yêu cầu với tùy chọn mặc định: được chia tối đa 3 hàng, mọi loại ghế, top 5. */
    public static SuggestionRequest of(int partySize) {
        return new SuggestionRequest(partySize, true, DEFAULT_MAX_ROWS, Set.of(), null, DEFAULT_TOP_K, null);
    }

    public SuggestionRequest withAllowSplit(boolean value) {
        return new SuggestionRequest(partySize, value, maxRows, allowedSeatTypes, maxPricePerPerson, topK, weights);
    }

    public SuggestionRequest withMaxRows(int value) {
        return new SuggestionRequest(partySize, allowSplit, value, allowedSeatTypes, maxPricePerPerson, topK, weights);
    }

    public SuggestionRequest withAllowedSeatTypes(Set<String> value) {
        return new SuggestionRequest(partySize, allowSplit, maxRows, value, maxPricePerPerson, topK, weights);
    }

    public SuggestionRequest withMaxPricePerPerson(Long value) {
        return new SuggestionRequest(partySize, allowSplit, maxRows, allowedSeatTypes, value, topK, weights);
    }

    public SuggestionRequest withTopK(int value) {
        return new SuggestionRequest(partySize, allowSplit, maxRows, allowedSeatTypes, maxPricePerPerson, value, weights);
    }

    public SuggestionRequest withWeights(ScoringWeights value) {
        return new SuggestionRequest(partySize, allowSplit, maxRows, allowedSeatTypes, maxPricePerPerson, topK, value);
    }

    boolean allows(SeatSpec seat) {
        return allowedSeatTypes.isEmpty() || allowedSeatTypes.contains(seat.typeCode());
    }
}
