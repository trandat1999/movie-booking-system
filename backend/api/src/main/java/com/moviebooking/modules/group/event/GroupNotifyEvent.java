package com.moviebooking.modules.group.event;

import java.util.List;

/** Thông báo in-app cho các thành viên, gửi sau commit (GRP18). Liên kết luôn trỏ tới trang nhóm. */
public record GroupNotifyEvent(Long groupId, List<Long> recipients, String type, String title, String body) {}
