package com.example.team8salecommerce.domain.chat.repository;

import com.example.team8salecommerce.domain.chat.entity.ChatRoom;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    List<ChatRoom> findAllByCreatedByIdOrderByCreatedAtDesc(Long memberId);
}
