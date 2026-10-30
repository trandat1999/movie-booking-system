package com.moviebooking.modules.payment.repository;

import com.moviebooking.modules.payment.entity.Refund;
import com.moviebooking.modules.payment.enums.RefundStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefundRepository extends JpaRepository<Refund, Long> {

    Optional<Refund> findByOrderId(Long orderId);

    Page<Refund> findByStatusOrderByCreatedAtAsc(RefundStatus status, Pageable pageable);

    Page<Refund> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
