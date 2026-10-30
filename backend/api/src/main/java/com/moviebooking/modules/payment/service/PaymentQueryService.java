package com.moviebooking.modules.payment.service;

import com.moviebooking.modules.payment.dto.response.PaymentView;
import java.util.List;

/** Tra cứu giao dịch cho màn quản trị đơn (không phụ thuộc module đặt vé để tránh vòng phụ thuộc). */
public interface PaymentQueryService {

    List<PaymentView> listByOrder(Long orderId);
}
