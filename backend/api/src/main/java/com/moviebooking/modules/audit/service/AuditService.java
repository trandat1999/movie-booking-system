package com.moviebooking.modules.audit.service;

import com.moviebooking.common.response.PageResponse;
import com.moviebooking.modules.audit.dto.request.AuditEntry;
import com.moviebooking.modules.audit.dto.response.AuditLogResponse;

public interface AuditService {

    /**
     * Ghi nhật ký trong transaction riêng: vẫn được lưu kể cả khi nghiệp vụ gọi nó bị rollback (ví dụ đăng nhập
     * thất bại). Lỗi khi ghi nhật ký không làm hỏng nghiệp vụ chính.
     */
    void record(AuditEntry entry);

    PageResponse<AuditLogResponse> search(String action, String resource, Long actorId, int page, int size);
}
