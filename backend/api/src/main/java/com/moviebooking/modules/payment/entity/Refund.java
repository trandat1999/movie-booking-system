package com.moviebooking.modules.payment.entity;

import com.moviebooking.modules.payment.enums.RefundStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** Yêu cầu hoàn tiền của một đơn (mỗi đơn tối đa một lần hoàn — không hủy một phần, D9). */
@Getter
@Setter
@Entity
@Table(name = "refunds")
public class Refund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, unique = true)
    private Long orderId;

    @Column(name = "payment_id")
    private Long paymentId;

    private String provider;

    @Column(nullable = false)
    private long amount;

    /** CUSTOMER_CANCEL, GROUP_CANCEL, SHOWTIME_CANCELLED, SEATS_UNAVAILABLE */
    @Column(nullable = false)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RefundStatus status;

    @Column(name = "provider_ref")
    private String providerRef;

    @Column(name = "requested_by")
    private Long requestedBy;

    @Column(name = "processed_by")
    private Long processedBy;

    private String note;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "processed_at")
    private Instant processedAt;
}
