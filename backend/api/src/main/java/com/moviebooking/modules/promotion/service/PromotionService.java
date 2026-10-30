package com.moviebooking.modules.promotion.service;

import com.moviebooking.common.response.PageResponse;
import com.moviebooking.modules.promotion.dto.request.PromotionRequest;
import com.moviebooking.modules.promotion.dto.response.PromotionResponse;

/**
 * Mã giảm giá (PROADM, BKG05). Áp mã giữ một lượt dùng ngay (khóa dòng mã, đếm lượt nguyên tử); đơn hủy/hết hạn
 * thì trả lượt. Module đặt vé gọi {@link #reserve}/{@link #release}; module này không phụ thuộc ngược.
 */
public interface PromotionService {

    PageResponse<PromotionResponse> search(String q, int page, int size);

    PromotionResponse get(Long id);

    PromotionResponse create(PromotionRequest request);

    PromotionResponse update(Long id, PromotionRequest request);

    /**
     * Kiểm tra điều kiện và giữ một lượt dùng cho đơn (thay lượt cũ nếu đơn đã áp mã khác).
     *
     * @param tickets số vé của đơn (ưu đãi nhóm theo {@code min_tickets})
     * @return số tiền giảm
     * @throws com.moviebooking.exception.BusinessException PROMOTION_INVALID, PROMOTION_NOT_APPLICABLE,
     *     PROMOTION_USED_UP
     */
    Applied reserve(Long userId, Long orderId, String code, long subtotal, int tickets);

    /** Trả lượt dùng của đơn (nếu có). */
    void release(Long orderId);

    record Applied(String code, String name, long discount) {}
}
