package com.moviebooking.modules.payment.repository;

import com.moviebooking.modules.payment.entity.Payment;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * Ghi giao dịch nếu chưa có; callback/IPN lặp lại của cùng giao dịch không tạo dòng mới (BR05).
     *
     * @return 1 nếu đã ghi, 0 nếu giao dịch đã được xử lý trước đó
     */
    @Modifying
    @Query(
            value = "INSERT INTO payments (order_id, provider, provider_txn_id, amount, status, raw, created_at) "
                    + "VALUES (:orderId, :provider, :txnId, :amount, :status, CAST(:raw AS jsonb), :createdAt) "
                    + "ON CONFLICT (provider, provider_txn_id) DO NOTHING",
            nativeQuery = true)
    int insertIfAbsent(
            @Param("orderId") Long orderId,
            @Param("provider") String provider,
            @Param("txnId") String txnId,
            @Param("amount") long amount,
            @Param("status") String status,
            @Param("raw") String raw,
            @Param("createdAt") Instant createdAt);

    List<Payment> findByOrderIdOrderByCreatedAtAsc(Long orderId);
}
