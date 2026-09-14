package com.hoadiemcat.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Cấu hình hệ thống WebSocket STOMP thời gian thực cho nhà hàng Hỏa Diệm Các.
 * Hỗ trợ giao tiếp 2 chiều giữa Khách hàng, Bếp KDS và Quản lý:
 * - /topic/table/{sessionToken}/cart : Đồng bộ giỏ hàng chung của bàn (UC04)
 * - /topic/kitchen/orders           : Bắn thông báo đơn món mới vào bếp FIFO (UC05, UC17)
 * - /topic/table/{sessionToken}/status: Cập nhật trạng thái chế biến món (UC06, UC18)
 * - /topic/admin/notifications      : Chuông gọi phục vụ và yêu cầu thanh toán (UC07, UC08, UC14)
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Kích hoạt Simple Broker với prefix /topic (broadcast pub-sub) và /queue (point-to-point)
        registry.enableSimpleBroker("/topic", "/queue");

        // Tiền tố cho các message gửi từ client lên server xử lý qua @MessageMapping
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // STOMP endpoint qua SockJS cho trình duyệt
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();

        // STOMP endpoint thuần không qua SockJS (fallback cho các client WebSocket native)
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");
    }
}
