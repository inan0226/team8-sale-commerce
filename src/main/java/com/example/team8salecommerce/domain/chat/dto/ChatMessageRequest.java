package com.example.team8salecommerce.domain.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChatMessageRequest(
        @NotNull(message = "채팅방 ID는 필수입니다.")
        Long chatRoomId,

        @NotBlank(message = "채팅 메시지는 필수입니다.")
        @Size(max = 1000, message = "채팅 메시지는 1000자 이하로 입력해야 합니다.")
        String content
) {
}
