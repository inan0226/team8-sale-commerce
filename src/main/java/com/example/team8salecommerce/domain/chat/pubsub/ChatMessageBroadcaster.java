package com.example.team8salecommerce.domain.chat.pubsub;

import com.example.team8salecommerce.domain.chat.dto.ChatMessageResponse;

/**
 * 저장된 채팅 메시지를 실시간 구독자에게 전달하는 공통 인터페이스입니다.
 *
 * <p>이 인터페이스를 둔 이유는 "메시지를 보내는 방법"을 컨트롤러가 직접 알지 않게 하기 위해서입니다.
 * 컨트롤러는 메시지를 저장한 뒤 {@link #broadcast(ChatMessageResponse)}만 호출하고,
 * 실제 전송 방식은 구현체가 선택합니다.</p>
 *
 * <p>현재 구현체는 두 가지입니다.</p>
 * <ul>
 *     <li>{@link RedisChatMessagePublisher}: 운영 기본값입니다. Redis Pub/Sub으로 여러 서버에 메시지를 전달합니다.</li>
 *     <li>{@link LocalChatMessageBroadcaster}: 테스트/로컬용입니다. Redis 없이 현재 서버의 STOMP 구독자에게 바로 전달합니다.</li>
 * </ul>
 */
public interface ChatMessageBroadcaster {

    /**
     * 저장이 끝난 채팅 메시지를 실시간 구독자에게 전달합니다.
     *
     * @param message DB 저장이 끝난 채팅 메시지 응답
     */
    void broadcast(ChatMessageResponse message);
}
