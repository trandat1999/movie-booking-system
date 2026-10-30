package com.moviebooking.modules.stats.service;

import com.moviebooking.modules.stats.dto.response.GroupStats;
import com.moviebooking.modules.stats.dto.response.OverviewStats;
import java.time.LocalDate;

/** Thống kê và báo cáo (ADM01–ADM03). Khoảng ngày theo giờ Việt Nam, gồm cả hai đầu; mặc định 30 ngày gần nhất. */
public interface StatsService {

    OverviewStats overview(LocalDate from, LocalDate to, Long cinemaId, Long movieId);

    GroupStats groups(LocalDate from, LocalDate to, Long movieId);

    /** CSV (UTF-8 có BOM để Excel đọc đúng tiếng Việt) các đơn tạo trong khoảng. */
    byte[] exportOrdersCsv(LocalDate from, LocalDate to, Long cinemaId, Long movieId);
}
