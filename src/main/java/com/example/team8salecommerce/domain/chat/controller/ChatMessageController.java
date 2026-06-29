package com.example.team8salecommerce.domain.chat.controller;

import com.example.team8salecommerce.domain.chat.dto.ChatMessageRequest;
import com.example.team8salecommerce.domain.chat.dto.ChatMessageResponse;
import com.example.team8salecommerce.domain.chat.pubsub.ChatMessageBroadcaster;
import com.example.team8salecommerce.domain.chat.service.ChatService;
import com.example.team8salecommerce.global.security.AuthMember;
import com.example.team8salecommerce.global.security.AuthMemberResolver;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

/**
 * WebSocket/STOMP로 들어오는 채팅 메시지를 처리하는 컨트롤러입니다.
 *
 * <p>클라이언트가 {@code /pub/chat/message}로 메시지를 보내면 이 컨트롤러가 실행됩니다.
 * 이 컨트롤러는 메시지를 DB에 저장하는 일과, 저장된 메시지를 브로드캐스터에 넘기는 일만 담당합니다.</p>
 *
 * <p>다중 서버 환경을 생각하면 컨트롤러에서 바로 {@code SimpMessagingTemplate.convertAndSend()}를 호출하면 안 됩니다.
 * 그렇게 하면 "현재 서버에 연결된 사용자"에게만 메시지가 가고,
 * 다른 서버에 연결된 같은 채팅방 구독자는 메시지를 못 받을 수 있습니다.
 * 그래서 {@link ChatMessageBroadcaster}를 통해 Redis Pub/Sub 흐름으로 넘깁니다.</p>
 */
@Controller
@RequiredArgsConstructor
public class ChatMessageController {

    private final ChatService chatService;
    private final ChatMessageBroadcaster chatMessageBroadcaster;
    private final AuthMemberResolver authMemberResolver;

    /**
     * 클라이언트가 보낸 채팅 메시지를 저장하고 실시간 구독자에게 전달합니다.
     *
     * <p>처리 순서는 다음과 같습니다.</p>
     * <ol>
     *     <li>STOMP CONNECT 때 저장된 Principal에서 로그인 회원을 꺼냅니다.</li>
     *     <li>{@link ChatService#sendMessage(Long, Long, ChatMessageRequest)}에서 방 접근 권한을 확인하고 메시지를 저장합니다.</li>
     *     <li>{@link ChatMessageBroadcaster}가 Redis Pub/Sub 또는 로컬 전송 방식으로 메시지를 전달합니다.</li>
     * </ol>
     */
    @MessageMapping("/chat/message")
    public void sendMessage(
            @Valid ChatMessageRequest request,
            Principal principal
    ) {
        AuthMember authMember = authMemberResolver.require(principal);
        ChatMessageResponse response = chatService.sendMessage(request.chatRoomId(), authMember.memberId(), request);

        chatMessageBroadcaster.broadcast(response);
    }
}
