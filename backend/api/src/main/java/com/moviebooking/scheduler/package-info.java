/**
 * Job định kỳ ({@code @Scheduled} + {@code @SchedulerLock}): giải phóng ghế hết hạn, hết hạn nhóm, hoàn tất
 * đơn sau suất chiếu. Job chỉ gọi service, không truy cập repository trực tiếp.
 */
package com.moviebooking.scheduler;
