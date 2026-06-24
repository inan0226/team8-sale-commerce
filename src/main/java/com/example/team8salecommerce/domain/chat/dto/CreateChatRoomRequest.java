package com.example.team8salecommerce.domain.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateChatRoomRequest(
        @NotBlank(message = "Chat room name is required.")
        @Size(max = 100, message = "Chat room name must be 100 characters or fewer.")
        String name
) {
}
