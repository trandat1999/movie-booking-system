package com.moviebooking.modules.booking.repository;

import com.moviebooking.modules.booking.entity.OrderEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderEventRepository extends JpaRepository<OrderEvent, Long> {

    List<OrderEvent> findByOrderIdOrderByCreatedAtAscIdAsc(Long orderId);
}
