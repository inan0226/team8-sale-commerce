package com.example.team8salecommerce.domain.chat.dto;

import com.example.team8salecommerce.domain.chat.entity.ChatRoomStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateChatRoomStatusRequest(
        @NotNull(message = "Chat room status is required.")
        ChatRoomStatus status
) {
}
