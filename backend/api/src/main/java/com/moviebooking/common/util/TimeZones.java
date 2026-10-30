package com.moviebooking.common.util;

import java.time.ZoneId;

public final class TimeZones {

    /** Múi giờ nghiệp vụ: ngày chiếu, loại ngày (thường/cuối tuần/lễ) và lọc theo ngày tính theo giờ Việt Nam. */
    public static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");

    private TimeZones() {}
}
