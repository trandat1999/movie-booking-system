package com.moviebooking.modules.booking.repository;

import com.moviebooking.modules.booking.entity.SeatHold;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SeatHoldRepository extends JpaRepository<SeatHold, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from SeatHold h where h.id = :id")
    Optional<SeatHold> findForUpdate(@Param("id") Long id);

    @Query("select h.id from SeatHold h where h.userId = :userId and h.showtimeId = :showtimeId and h.groupId is null "
            + "and h.status = com.moviebooking.modules.booking.enums.HoldStatus.ACTIVE")
    List<Long> findActiveIds(@Param("userId") Long userId, @Param("showtimeId") Long showtimeId);

    /** Số lượt giữ cá nhân còn hiệu lực của người dùng (BR15) — lượt giữ của nhóm không tính. */
    @Query("select count(h) from SeatHold h where h.userId = :userId and h.groupId is null "
            + "and h.status = com.moviebooking.modules.booking.enums.HoldStatus.ACTIVE and h.expiresAt > :now")
    long countActive(@Param("userId") Long userId, @Param("now") Instant now);

    /** Lượt giữ đã quá hạn nhưng chưa được dọn — cho job giải phóng ghế. */
    @Query("select h.id from SeatHold h where h.status = com.moviebooking.modules.booking.enums.HoldStatus.ACTIVE "
            + "and h.expiresAt < :now order by h.expiresAt")
    List<Long> findDueIds(@Param("now") Instant now, Pageable pageable);
}
