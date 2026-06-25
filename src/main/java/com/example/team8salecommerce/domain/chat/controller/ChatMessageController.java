package com.example.team8salecommerce.domain.chat.controller;

import com.example.team8salecommerce.domain.chat.dto.ChatMessageRequest;
import com.example.team8salecommerce.domain.chat.dto.ChatMessageResponse;
import com.example.team8salecommerce.domain.chat.service.ChatService;
import com.example.team8salecommerce.global.exception.CustomException;
import com.example.team8salecommerce.global.exception.ErrorCode;
import com.example.team8salecommerce.global.security.AuthMember;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatMessageController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat/message")
    public void sendMessage(
            @Valid ChatMessageRequest request,
            Principal principal
    ) {
        AuthMember authMember = resolveAuthMember(principal);
        ChatMessageResponse response = chatService.sendMessage(request.chatRoomId(), authMember.memberId(), request);

        messagingTemplate.convertAndSend("/sub/chat/rooms/" + request.chatRoomId(), response);
    }

    private AuthMember resolveAuthMember(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken authentication
                && authentication.getPrincipal() instanceof AuthMember authMember) {
            return authMember;
        }

        throw new CustomException(ErrorCode.UNAUTHORIZED);
    }
}
