package com.moviebooking.modules.audit.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moviebooking.common.response.PageResponse;
import com.moviebooking.common.web.ClientInfo;
import com.moviebooking.modules.audit.dto.request.AuditEntry;
import com.moviebooking.modules.audit.dto.response.AuditLogResponse;
import com.moviebooking.modules.audit.entity.AuditLog;
import com.moviebooking.modules.audit.mapper.AuditLogMapper;
import com.moviebooking.modules.audit.repository.AuditLogRepository;
import com.moviebooking.modules.audit.service.AuditService;
import com.moviebooking.security.AuthenticatedUser;
import java.time.Clock;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
public class AuditServiceImpl implements AuditService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final TypeReference<Map<String, Object>> JSON_OBJECT = new TypeReference<>() {};

    private final AuditLogRepository auditLogRepository;
    private final AuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final TransactionTemplate requiresNewTransaction;

    public AuditServiceImpl(
            AuditLogRepository auditLogRepository,
            AuditLogMapper auditLogMapper,
            ObjectMapper objectMapper,
            Clock clock,
            PlatformTransactionManager transactionManager) {
        this.auditLogRepository = auditLogRepository;
        this.auditLogMapper = auditLogMapper;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.requiresNewTransaction = new TransactionTemplate(transactionManager);
        this.requiresNewTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public void record(AuditEntry entry) {
        try {
            AuditLog auditLog = toEntity(entry);
            requiresNewTransaction.executeWithoutResult(status -> auditLogRepository.save(auditLog));
        } catch (RuntimeException ex) {
            log.error("Failed to write audit log {}", entry.action(), ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> search(String action, String resource, Long actorId, int page, int size) {
        Specification<AuditLog> spec = Specification.allOf(
                equalTo("action", action), equalTo("resource", resource), equalTo("actorId", actorId));
        PageRequest pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id")));
        return PageResponse.from(auditLogRepository.findAll(spec, pageable).map(auditLogMapper::toResponse));
    }

    private AuditLog toEntity(AuditEntry entry) {
        AuthenticatedUser current = currentUser();
        AuditLog auditLog = new AuditLog();
        auditLog.setAction(entry.action());
        auditLog.setResource(entry.resource());
        auditLog.setResourceId(entry.resourceId());
        auditLog.setData(toJsonObject(entry.data()));
        auditLog.setActorId(entry.actorId() != null ? entry.actorId() : current == null ? null : current.id());
        auditLog.setActorEmail(
                entry.actorEmail() != null ? entry.actorEmail() : current == null ? null : current.email());
        auditLog.setIpAddress(entry.ipAddress() != null ? entry.ipAddress() : ClientInfo.current().ipAddress());
        auditLog.setCreatedAt(clock.instant());
        return auditLog;
    }

    /**
     * Chuyển dữ liệu về Map thuần bằng ObjectMapper của Spring (ngày giờ ghi dạng ISO-8601). Giá trị không phải
     * object (chuỗi, số, danh sách) được bọc thành {@code {"value": ...}}.
     */
    private Map<String, Object> toJsonObject(Object data) {
        if (data == null) {
            return null;
        }
        JsonNode node = objectMapper.valueToTree(data);
        return node.isObject()
                ? objectMapper.convertValue(node, JSON_OBJECT)
                : Map.of("value", objectMapper.convertValue(node, Object.class));
    }

    private static Specification<AuditLog> equalTo(String attribute, Object value) {
        if (value == null || (value instanceof String s && s.isBlank())) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get(attribute), value);
    }

    private static AuthenticatedUser currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getPrincipal() instanceof AuthenticatedUser user ? user : null;
    }
}
