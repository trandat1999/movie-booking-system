package com.moviebooking.security;

import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
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
 *   <li>đăng ký {@code /user/**} khi chưa đăng nhập.
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

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
        }
        return message;
    }
}
