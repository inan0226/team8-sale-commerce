package com.example.team8salecommerce.domain.chat.dto;

import com.example.team8salecommerce.domain.chat.entity.ChatRoom;
import com.example.team8salecommerce.domain.chat.entity.ChatRoomStatus;
import java.time.LocalDateTime;

public record ChatRoomResponse(
        Long id,
        String name,
        Long createdById,
        String createdByNickname,
        ChatRoomStatus status,
        LocalDateTime createdAt
) {

    public static ChatRoomResponse from(ChatRoom chatRoom) {
        return new ChatRoomResponse(
                chatRoom.getId(),
                chatRoom.getName(),
                chatRoom.getCreatedBy().getId(),
                chatRoom.getCreatedBy().getNickname(),
                chatRoom.getStatus(),
                chatRoom.getCreatedAt()
        );
    }
}
