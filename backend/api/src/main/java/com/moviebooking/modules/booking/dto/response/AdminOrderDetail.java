package com.moviebooking.modules.booking.dto.response;

import com.moviebooking.modules.booking.enums.OrderStatus;
import com.moviebooking.modules.payment.dto.response.PaymentView;
import com.moviebooking.modules.user.dto.response.UserBrief;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Chi tiết đơn cho quản trị: đơn, khách, lịch sử thay đổi (BR09), giao dịch để đối soát. */
public record AdminOrderDetail(
        OrderResponse order, UserBrief customer, List<EventView> events, List<PaymentView> payments) {

    public record EventView(
            String type, OrderStatus fromStatus, OrderStatus toStatus, Long actorId, Map<String, Object> data, Instant createdAt) {}
}
