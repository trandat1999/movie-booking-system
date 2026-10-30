package com.moviebooking.modules.booking.dto.request;

import com.moviebooking.modules.booking.enums.OrderStatus;
import java.time.LocalDate;

/**
 * Bộ lọc danh sách đơn (ORDADM).
 *
 * @param q mã đơn (một phần)
 * @param email email khách (chính xác)
 * @param from ngày tạo đơn từ (giờ VN)
 */
public record AdminOrderQuery(
        String q, String email, OrderStatus status, LocalDate from, LocalDate to, Long cinemaId, Long movieId, int page, int size) {}
