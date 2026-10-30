package com.moviebooking.modules.booking.repository;

import com.moviebooking.modules.booking.entity.Ticket;
import java.time.Instant;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByOrderIdOrderByIdAsc(Long orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Ticket t join fetch t.order o join fetch o.showtime where t.code = :code")
    Optional<Ticket> findForUpdateByCode(@Param("code") String code);

    /** Vé của người dùng cho các suất chưa kết thúc, suất gần nhất trước (Vé của tôi). */
    @Query("select t from Ticket t join fetch t.order o join fetch o.showtime s join fetch s.movie "
            + "join fetch s.room r join fetch r.cinema "
            + "where t.holderUserId = :userId and t.status <> com.moviebooking.modules.booking.enums.TicketStatus.CANCELLED "
            + "and s.endAt > :now order by s.startAt, t.id")
    List<Ticket> findUpcomingForUser(@Param("userId") Long userId, @Param("now") Instant now);

    /** Vé đã xem (suất đã kết thúc), gần nhất trước. */
    @Query("select t from Ticket t join fetch t.order o join fetch o.showtime s join fetch s.movie "
            + "join fetch s.room r join fetch r.cinema "
            + "where t.holderUserId = :userId and s.endAt <= :now order by s.startAt desc, t.id")
    List<Ticket> findPastForUser(@Param("userId") Long userId, @Param("now") Instant now);

    /** Người dùng có vé đã soát của phim (điều kiện viết đánh giá — FBK01). */
    @Query("select count(t) > 0 from Ticket t join t.order o join o.showtime s "
            + "where t.holderUserId = :userId and s.movie.id = :movieId "
            + "and t.status = com.moviebooking.modules.booking.enums.TicketStatus.USED")
    boolean existsUsedForMovie(@Param("userId") Long userId, @Param("movieId") Long movieId);
}
