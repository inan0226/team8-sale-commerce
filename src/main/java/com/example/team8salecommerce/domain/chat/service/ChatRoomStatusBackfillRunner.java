package com.example.team8salecommerce.domain.chat.service;

import com.example.team8salecommerce.domain.chat.entity.ChatRoomStatus;
import com.example.team8salecommerce.domain.chat.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ChatRoomStatusBackfillRunner {

    private final ChatRoomRepository chatRoomRepository;

    @Transactional
    @EventListener(ApplicationReadyEvent.class)
    public void backfillMissingStatus() {
        chatRoomRepository.updateNullStatus(ChatRoomStatus.WAITING);
    }
}
