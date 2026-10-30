package com.moviebooking.modules.promotion.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** Một lượt dùng mã: giữ chỗ khi áp mã vào đơn, xóa (trả lượt) khi đơn hủy/hết hạn. */
@Getter
@Setter
@Entity
@Table(name = "promotion_usages")
public class PromotionUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "promotion_id", nullable = false)
    private Long promotionId;

    @Column(name = "order_id", nullable = false, unique = true)
    private Long orderId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private long discount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
