package com.example.team8salecommerce.domain.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateChatRoomRequest(
        @NotBlank(message = "채팅방 이름은 필수입니다.")
        @Size(max = 100, message = "채팅방 이름은 100자 이하로 입력해야 합니다.")
        String name
) {
}
