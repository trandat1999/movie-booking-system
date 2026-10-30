package com.moviebooking.modules.audit.mapper;

import com.moviebooking.modules.audit.dto.response.AuditLogResponse;
import com.moviebooking.modules.audit.entity.AuditLog;
import org.mapstruct.Mapper;

@Mapper
public interface AuditLogMapper {

    AuditLogResponse toResponse(AuditLog auditLog);
}
