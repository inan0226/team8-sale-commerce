package com.example.team8salecommerce.domain.chat.repository;

import com.example.team8salecommerce.domain.chat.entity.ChatRoom;
import com.example.team8salecommerce.domain.chat.entity.ChatRoomStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    List<ChatRoom> findAllByCreatedByIdOrderByCreatedAtDesc(Long memberId);

    List<ChatRoom> findAllByOrderByCreatedAtDesc();

    Optional<ChatRoom> findFirstByCreatedByIdAndStatusNotOrderByCreatedAtDesc(
            Long memberId,
            ChatRoomStatus status
    );
}
