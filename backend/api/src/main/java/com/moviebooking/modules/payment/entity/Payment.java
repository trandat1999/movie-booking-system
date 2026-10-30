package com.moviebooking.modules.payment.entity;

import com.moviebooking.modules.payment.enums.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Giao dịch nhận từ cổng thanh toán; {@code (provider, providerTxnId)} là duy nhất để chống xử lý trùng (BR05). */
@Getter
@Setter
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(nullable = false)
    private String provider;

    @Column(name = "provider_txn_id", nullable = false)
    private String providerTxnId;

    @Column(nullable = false)
    private long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> raw;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
