package com.example.team8salecommerce.global.websocket;

import com.example.team8salecommerce.global.security.SecurityConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * 채팅 WebSocket과 STOMP 메시지 브로커를 설정하는 클래스입니다.
 *
 * <p>HTTP API는 일반 컨트롤러로 처리하지만, 채팅처럼 실시간 양방향 통신이 필요한 기능은
 * WebSocket 연결을 사용합니다. 이 설정은 클라이언트가 어디로 연결하고,
 * 어떤 경로로 메시지를 보내고 구독할지 정합니다.</p>
 *
 * <p>중요한 점은 {@code /ws/chat} 연결 자체는 {@link SecurityConfig}에서 허용하지만,
 * 실제 STOMP 메시지의 JWT 인증과 채팅방 인가는 {@link WebSocketJwtChannelInterceptor}에서 처리한다는 점입니다.</p>
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketJwtChannelInterceptor webSocketJwtChannelInterceptor;

    @Value("${websocket.allowed-origin-patterns:*}")
    private String[] allowedOriginPatterns;

    /**
     * 클라이언트가 WebSocket 연결을 시작할 엔드포인트를 등록합니다.
     *
     * <p>브라우저나 프론트엔드는 먼저 {@code /ws/chat}으로 연결(handshake)을 맺습니다.
     * 이 단계는 아직 STOMP CONNECT 프레임이 아니므로, JWT 검증은 inbound channel interceptor에서 진행됩니다.</p>
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/chat")
                .setAllowedOriginPatterns(allowedOriginPatterns);
    }

    /**
     * STOMP 메시지 경로 규칙을 설정합니다.
     *
     * <p>클라이언트가 서버로 메시지를 보낼 때는 {@code /pub/**} 경로를 사용하고,
     * 서버가 브로드캐스트하는 메시지를 받을 때는 {@code /sub/**} 경로를 구독합니다.</p>
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/sub");
        registry.setApplicationDestinationPrefixes("/pub");
    }

    /**
     * 클라이언트가 서버로 보내는 모든 STOMP 프레임에 인터셉터를 적용합니다.
     *
     * <p>여기서 등록한 {@link WebSocketJwtChannelInterceptor}가 CONNECT, SEND, SUBSCRIBE 프레임을 검사해
     * 인증되지 않은 사용자나 접근 권한이 없는 채팅방 구독을 차단합니다.</p>
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(webSocketJwtChannelInterceptor);
    }
}
