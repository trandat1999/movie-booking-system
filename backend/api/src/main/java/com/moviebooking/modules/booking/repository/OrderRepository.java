package com.moviebooking.modules.booking.repository;

import com.moviebooking.modules.booking.entity.Order;
import jakarta.persistence.LockModeType;
import com.moviebooking.modules.booking.enums.OrderStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from BookingOrder o where o.id = :id")
    Optional<Order> findForUpdateById(@Param("id") Long id);

    @Query("select o.id from BookingOrder o where o.showtime.id = :showtimeId and o.status in :statuses order by o.id")
    List<Long> findIdsByShowtimeAndStatuses(
            @Param("showtimeId") Long showtimeId, @Param("statuses") Collection<OrderStatus> statuses);

    /** PAID → COMPLETED cho đơn của suất đã chiếu xong (PLAN.md 6.2). */
    @Modifying
    @Query("update BookingOrder o set o.status = com.moviebooking.modules.booking.enums.OrderStatus.COMPLETED "
            + "where o.status = com.moviebooking.modules.booking.enums.OrderStatus.PAID "
            + "and o.showtime.id in (select s.id from Showtime s where s.endAt < :now)")
    int completeFinished(@Param("now") Instant now);

    Optional<Order> findByHoldId(Long holdId);

    Optional<Order> findByCode(String code);

    @EntityGraph(attributePaths = {"showtime", "showtime.movie", "showtime.room", "showtime.room.cinema"})
    Page<Order> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
