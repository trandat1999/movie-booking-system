package com.moviebooking.modules.booking.repository;

import com.moviebooking.modules.booking.entity.Order;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Thứ tự khóa trong module đặt vé để tránh deadlock: đơn hàng → lượt giữ → dòng ghế ({@code showtime_seats}).
 */
public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = {"showtime", "showtime.movie", "showtime.room", "showtime.room.cinema", "items", "tickets"})
    Optional<Order> findWithDetailsByCode(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from BookingOrder o where o.code = :code")
    Optional<Order> findForUpdateByCode(@Param("code") String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from BookingOrder o where o.holdId = :holdId")
    Optional<Order> findForUpdateByHoldId(@Param("holdId") Long holdId);

    Optional<Order> findByHoldId(Long holdId);

    Optional<Order> findByCode(String code);

    @EntityGraph(attributePaths = {"showtime", "showtime.movie", "showtime.room", "showtime.room.cinema"})
    Page<Order> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
