package com.example.team8salecommerce.domain.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChatMessageRequest(
        @NotNull(message = "Chat room id is required.")
        Long chatRoomId,

        @NotBlank(message = "Chat message is required.")
        @Size(max = 1000, message = "Chat message must be 1000 characters or fewer.")
        String content
) {
}
