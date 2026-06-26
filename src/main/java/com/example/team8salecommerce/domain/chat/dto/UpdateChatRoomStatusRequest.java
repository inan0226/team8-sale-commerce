package com.example.team8salecommerce.domain.chat.dto;

import com.example.team8salecommerce.domain.chat.entity.ChatRoomStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateChatRoomStatusRequest(
        @NotNull(message = "채팅방 상태는 필수입니다.")
        ChatRoomStatus status
) {
}
