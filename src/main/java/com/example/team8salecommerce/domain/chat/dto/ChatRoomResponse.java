package com.example.team8salecommerce.domain.chat.dto;

import com.example.team8salecommerce.domain.chat.entity.ChatRoom;
import java.time.LocalDateTime;

public record ChatRoomResponse(
        Long id,
        String name,
        Long createdById,
        String createdByNickname,
        LocalDateTime createdAt
) {

    public static ChatRoomResponse from(ChatRoom chatRoom) {
        return new ChatRoomResponse(
                chatRoom.getId(),
                chatRoom.getName(),
                chatRoom.getCreatedBy().getId(),
                chatRoom.getCreatedBy().getNickname(),
                chatRoom.getCreatedAt()
        );
    }
}
