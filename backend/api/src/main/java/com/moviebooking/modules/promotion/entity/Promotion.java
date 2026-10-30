package com.moviebooking.modules.promotion.entity;

import com.moviebooking.common.entity.BaseEntity;
import com.moviebooking.modules.promotion.enums.PromotionStatus;
import com.moviebooking.modules.promotion.enums.PromotionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** Mã giảm giá (PROADM). {@link #minTickets} &gt; 1 là ưu đãi theo số vé (đặt vé nhóm). */
@Getter
@Setter
@Entity
@Table(name = "promotions")
public class Promotion extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PromotionType type;

    /** PERCENT: phần trăm (1–100); AMOUNT: số tiền (VND). */
    @Column(nullable = false)
    private long value;

    @Column(name = "max_discount")
    private Long maxDiscount;

    @Column(name = "min_order", nullable = false)
    private long minOrder;

    @Column(name = "min_tickets", nullable = false)
    private int minTickets = 1;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Column(name = "usage_limit")
    private Integer usageLimit;

    @Column(name = "per_user_limit")
    private Integer perUserLimit;

    @Column(name = "used_count", nullable = false)
    private int usedCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PromotionStatus status = PromotionStatus.ACTIVE;

    /** Số tiền giảm cho tạm tính {@code subtotal}, không vượt tạm tính. */
    public long discountFor(long subtotal) {
        long discount = type == PromotionType.PERCENT ? subtotal * value / 100 : value;
        if (maxDiscount != null) {
            discount = Math.min(discount, maxDiscount);
        }
        return Math.min(discount, subtotal);
    }
}
