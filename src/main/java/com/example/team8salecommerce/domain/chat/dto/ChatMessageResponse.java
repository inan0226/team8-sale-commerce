package com.example.team8salecommerce.domain.chat.dto;

import com.example.team8salecommerce.domain.chat.entity.ChatMessage;
import java.time.LocalDateTime;

public record ChatMessageResponse(
        Long id,
        Long roomId,
        Long senderId,
        String senderNickname,
        String content,
        LocalDateTime createdAt
) {

    public static ChatMessageResponse from(ChatMessage chatMessage) {
        return new ChatMessageResponse(
                chatMessage.getId(),
                chatMessage.getChatRoom().getId(),
                chatMessage.getSender().getId(),
                chatMessage.getSender().getNickname(),
                chatMessage.getContent(),
                chatMessage.getCreatedAt()
        );
    }
}
