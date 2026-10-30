package com.moviebooking.security;

import com.moviebooking.modules.group.service.GroupService;
import io.jsonwebtoken.JwtException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Xác thực kết nối STOMP: header {@code Authorization: Bearer <token>} trong frame CONNECT (tùy chọn — khách chưa
 * đăng nhập vẫn xem sơ đồ ghế realtime). Chặn:
 *
 * <ul>
 *   <li>gửi lệnh nghiệp vụ qua WebSocket (mọi thay đổi đi qua REST, PLAN.md 8.3);
 *   <li>đăng ký {@code /user/**} khi chưa đăng nhập;
 *   <li>đăng ký {@code /topic/groups/{id}} khi không phải thành viên nhóm (PLAN.md 8.3).
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private static final Pattern GROUP_TOPIC = Pattern.compile("^/topic/groups/(\\d+)$");

    private final JwtService jwtService;
    /** Lazy để tránh vòng phụ thuộc WebSocketConfig → interceptor → GroupService → RealtimePublisher. */
    private final ObjectProvider<GroupService> groupService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }
        StompCommand command = accessor.getCommand();
        if (command == StompCommand.CONNECT) {
            String header = accessor.getFirstNativeHeader("Authorization");
            if (header != null && header.startsWith(BEARER_PREFIX)) {
                try {
                    AuthenticatedUser user = jwtService.parse(header.substring(BEARER_PREFIX.length()));
                    accessor.setUser(new UsernamePasswordAuthenticationToken(user, null, user.authorities()));
                } catch (JwtException | IllegalArgumentException ex) {
                    throw new MessageDeliveryException("Invalid access token");
                }
            }
        } else if (command == StompCommand.SEND) {
            throw new MessageDeliveryException("Sending messages over WebSocket is not supported");
        } else if (command == StompCommand.SUBSCRIBE) {
            String destination = accessor.getDestination();
            if (destination != null && destination.startsWith("/user/") && accessor.getUser() == null) {
                throw new MessageDeliveryException("Authentication required");
            }
            Matcher group = destination == null ? null : GROUP_TOPIC.matcher(destination);
            if (group != null && group.matches() && !isGroupMember(accessor, Long.valueOf(group.group(1)))) {
                throw new MessageDeliveryException("Not a member of this group");
            }
        }
        return message;
    }

    private boolean isGroupMember(StompHeaderAccessor accessor, Long groupId) {
        if (accessor.getUser() instanceof UsernamePasswordAuthenticationToken auth
                && auth.getPrincipal() instanceof AuthenticatedUser user) {
            return groupService.getObject().isActiveMember(groupId, user.id());
        }
        return false;
    }
}
