package com.moviebooking.modules.group.repository;

import com.moviebooking.modules.group.entity.BookingGroup;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Thứ tự khóa của luồng nhóm: dòng nhóm trước, sau đó mới tới đơn → lượt giữ → ghế (module đặt vé). Luồng ngược
 * (job hết hạn giữ ghế báo ngược về nhóm) có thể tranh khóa; PostgreSQL phát hiện deadlock và một bên được thử
 * lại (409 / lượt job sau).
 */
public interface BookingGroupRepository extends JpaRepository<BookingGroup, Long>, JpaSpecificationExecutor<BookingGroup> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from BookingGroup g where g.id = :id")
    Optional<BookingGroup> findForUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = {"movie", "cinema"})
    Optional<BookingGroup> findWithDetailsById(Long id);

    Optional<BookingGroup> findByCode(String code);

    boolean existsByCode(String code);

    /** Số nhóm đang hoạt động mà người dùng là thành viên (BR15). */
    @Query("select count(g) from BookingGroup g, GroupMember m where m.groupId = g.id and m.user.id = :userId "
            + "and m.leftAt is null and g.status in (com.moviebooking.modules.group.enums.GroupStatus.OPEN, "
            + "com.moviebooking.modules.group.enums.GroupStatus.LOCKED, "
            + "com.moviebooking.modules.group.enums.GroupStatus.HOLDING)")
    long countActiveForUser(@Param("userId") Long userId);

    /** Nhóm của tôi (GRP20): nhóm người dùng đang là thành viên, mới nhất trước. */
    @EntityGraph(attributePaths = {"movie", "cinema"})
    @Query("select g from BookingGroup g where g.id in "
            + "(select m.groupId from GroupMember m where m.user.id = :userId and m.leftAt is null) "
            + "order by g.createdAt desc")
    List<BookingGroup> findForMember(@Param("userId") Long userId);

    @Query("select g.id from BookingGroup g where g.lockedShowtimeId = :showtimeId and g.status in "
            + "(com.moviebooking.modules.group.enums.GroupStatus.LOCKED, "
            + "com.moviebooking.modules.group.enums.GroupStatus.HOLDING, "
            + "com.moviebooking.modules.group.enums.GroupStatus.BOOKED)")
    List<Long> findIdsByLockedShowtime(@Param("showtimeId") Long showtimeId);

    @Modifying
    @Query("update BookingGroup g set g.status = com.moviebooking.modules.group.enums.GroupStatus.COMPLETED "
            + "where g.status = com.moviebooking.modules.group.enums.GroupStatus.BOOKED "
            + "and g.lockedShowtimeId in (select s.id from Showtime s where s.endAt < :now)")
    int completeFinished(@Param("now") Instant now);

    /** Nhóm chưa chốt đã quá hạn chốt. */
    @Query("select g.id from BookingGroup g where g.status = com.moviebooking.modules.group.enums.GroupStatus.OPEN "
            + "and g.rsvpDeadline < :now")
    List<Long> findExpiredOpenIds(@Param("now") Instant now);

    /** Nhóm chưa chốt sắp tới hạn chốt và chưa nhắc. */
    @Query("select g.id from BookingGroup g where g.status = com.moviebooking.modules.group.enums.GroupStatus.OPEN "
            + "and g.reminderSentAt is null and g.rsvpDeadline >= :now and g.rsvpDeadline <= :before")
    List<Long> findReminderDueIds(@Param("now") Instant now, @Param("before") Instant before);
}
