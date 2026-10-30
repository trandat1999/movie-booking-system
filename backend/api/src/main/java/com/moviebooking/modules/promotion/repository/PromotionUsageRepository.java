package com.moviebooking.modules.promotion.repository;

import com.moviebooking.modules.promotion.entity.PromotionUsage;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromotionUsageRepository extends JpaRepository<PromotionUsage, Long> {

    Optional<PromotionUsage> findByOrderId(Long orderId);

    long countByPromotionIdAndUserId(Long promotionId, Long userId);
}
