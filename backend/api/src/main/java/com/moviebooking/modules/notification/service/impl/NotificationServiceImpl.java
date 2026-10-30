package com.moviebooking.modules.notification.service.impl;

import com.moviebooking.common.response.PageResponse;
import com.moviebooking.common.util.TimeZones;
import com.moviebooking.exception.BusinessException;
import com.moviebooking.exception.ErrorCode;
import com.moviebooking.infrastructure.mail.EmailSender;
import com.moviebooking.infrastructure.mail.MailProperties;
import com.moviebooking.infrastructure.realtime.RealtimePublisher;
import com.moviebooking.modules.booking.dto.response.OrderResponse;
import com.moviebooking.modules.booking.service.OrderService;
import com.moviebooking.modules.notification.dto.response.NotificationResponse;
import com.moviebooking.modules.notification.entity.Notification;
import com.moviebooking.modules.notification.repository.NotificationRepository;
import com.moviebooking.modules.notification.service.NotificationService;
import com.moviebooking.modules.user.entity.User;
import com.moviebooking.modules.user.service.UserService;
import java.time.Clock;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final DateTimeFormatter SHOW_TIME =
            DateTimeFormatter.ofPattern("HH:mm 'ngày' dd/MM/yyyy").withZone(TimeZones.VIETNAM);

    private final NotificationRepository notificationRepository;
    private final RealtimePublisher realtimePublisher;
    private final OrderService orderService;
    private final UserService userService;
    private final EmailSender emailSender;
    private final MailProperties mailProperties;
    private final ITemplateEngine templateEngine;
    private final Clock clock;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public NotificationResponse notify(Long userId, String type, String title, String body, String link) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setBody(body);
        notification.setLink(link);
        notification.setCreatedAt(clock.instant());
        notificationRepository.save(notification);
        NotificationResponse response = toResponse(notification);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                realtimePublisher.toUser(userId, "/queue/notifications", response);
            }
        });
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> list(Long userId, int page, int size) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
        return PageResponse.from(notificationRepository
                .findByUserIdOrderByCreatedAtDescIdDesc(userId, pageable)
                .map(this::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return notificationRepository.countByUserIdAndReadAtIsNull(userId);
    }

    @Override
    @Transactional
    public void markRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository
                .findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));
        if (notification.getReadAt() == null) {
            notification.setReadAt(clock.instant());
        }
    }

    @Override
    @Transactional
    public void markAllRead(Long userId) {
        notificationRepository.markAllRead(userId, clock.instant());
    }

    @Override
    @Async
    public void sendOrderPaidEmail(Long userId, String orderCode) {
        try {
            User user = userService.getWithRoles(userId);
            OrderResponse order = orderService.get(userId, orderCode);
            Context context = new Context();
            context.setVariable("fullName", user.getFullName());
            context.setVariable("order", order);
            context.setVariable("showTime", SHOW_TIME.format(order.showtime().startAt()));
            context.setVariable("orderUrl", mailProperties.webBaseUrl() + "/orders/" + orderCode);
            String html = templateEngine.process("mail/order-paid", context);
            emailSender.send(user.getEmail(), "Đặt vé thành công - " + order.showtime().movieTitle(), html);
        } catch (RuntimeException ex) {
            log.error("Failed to build order-paid email for order {}: {}", orderCode, ex.getMessage());
        }
    }

    private NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(
                n.getId(), n.getType(), n.getTitle(), n.getBody(), n.getLink(), n.getReadAt(), n.getCreatedAt());
    }
}
