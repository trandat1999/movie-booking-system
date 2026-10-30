package com.moviebooking.modules.booking.service;

import com.moviebooking.modules.booking.dto.request.HoldRequest;
import com.moviebooking.modules.booking.dto.response.HoldResponse;
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
