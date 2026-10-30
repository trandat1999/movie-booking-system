package com.moviebooking.modules.audit.service;

import com.moviebooking.common.response.PageResponse;
import com.moviebooking.modules.audit.dto.request.AuditEntry;
import com.moviebooking.modules.audit.dto.response.AuditLogResponse;

public interface AuditService {

    /**
     * Ghi nhật ký trong transaction hiện tại (hoặc transaction mới nếu chưa có): nghiệp vụ rollback thì nhật ký cũng
     * không còn — đúng ngữ nghĩa vì hành động không xảy ra. Nghiệp vụ cần giữ nhật ký khi từ chối (đăng nhập thất bại,
     * phát hiện dùng lại refresh token) khai báo {@code noRollbackFor = BusinessException.class}.
     *
     * <p>Không dùng REQUIRES_NEW: mỗi request sẽ giữ 2 kết nối cùng lúc và làm cạn pool khi tải cao (M9).
     */
    void record(AuditEntry entry);

    PageResponse<AuditLogResponse> search(String action, String resource, Long actorId, int page, int size);
}
