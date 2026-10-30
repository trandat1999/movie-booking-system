package com.moviebooking.modules.booking.service;

import com.moviebooking.modules.booking.dto.request.HoldRequest;
import com.moviebooking.modules.booking.dto.response.HoldResponse;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

/** Giữ ghế tạm thời khi đặt vé cá nhân (BKG02–BKG04, BR01–BR03, BR14, BR15). */
public interface HoldService {

    /**
     * Giữ nguyên tử toàn bộ ghế đã chọn — không giữ được một phần. Lượt giữ cũ của người dùng cho cùng suất (và
     * đơn chờ thanh toán của nó) bị thay thế.
     */
    HoldResponse hold(Long userId, HoldRequest request);

    /** Bỏ giữ ghế; đơn chờ thanh toán của lượt giữ (nếu có) bị hủy. */
    void release(Long userId, Long holdId);

    /**
     * Giữ nguyên khối ghế cho nhóm (GRP14, BR03): không áp quy tắc ghế lẻ/giới hạn cá nhân (thuật toán nhóm đã trừ
     * điểm ghế lẻ), thời hạn {@code hold_ttl_group_sec}. Nơi gọi (module nhóm) đã kiểm tra quyền trưởng nhóm.
     *
     * @throws com.moviebooking.exception.BusinessException SEAT_UNAVAILABLE nếu có ghế không giữ được — không ghế
     *     nào được giữ
     */
    GroupHold holdForGroup(Long leaderId, Long groupId, Long showtimeId, Collection<Long> seatIds);

    /** Bỏ lượt giữ của nhóm (trưởng nhóm chọn lại ghế / hủy nhóm). Không làm gì nếu lượt giữ đã đóng. */
    void releaseGroupHold(Long holdId, Long actorId);

    record GroupHold(Long holdId, Instant expiresAt) {}

    /** Lượt giữ còn hiệu lực (ACTIVE, chưa hết hạn) kèm ghế; rỗng nếu đã đóng/hết hạn. */
    java.util.Optional<ActiveHold> findActive(Long holdId);

    record ActiveHold(Long holdId, Instant expiresAt, List<Long> seatIds) {}

    /** Lượt giữ đã quá hạn nhưng chưa được dọn, cũ nhất trước. */
    List<Long> findDueIds(int limit);

    /**
     * Dọn một lượt giữ quá hạn: trả ghế, chuyển đơn chờ thanh toán sang EXPIRED. Không làm gì nếu lượt giữ không
     * còn ACTIVE hoặc chưa hết hạn.
     *
     * @return true nếu đã dọn
     */
    boolean expire(Long holdId);
}
