package com.example.team8salecommerce.domain.chat.entity;

import com.example.team8salecommerce.domain.member.entity.Member;
import com.example.team8salecommerce.global.util.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "chat_rooms")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class ChatRoom extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private Member createdBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChatRoomStatus status;

    public static ChatRoom create(String name, Member createdBy) {
        return ChatRoom.builder()
                .name(name.trim())
                .createdBy(createdBy)
                .status(ChatRoomStatus.WAITING)
                .build();
    }

    public boolean isClosed() {
        return status == ChatRoomStatus.CLOSED;
    }

    public void updateStatus(ChatRoomStatus nextStatus) {
        if (nextStatus == null || status == nextStatus) {
            return;
        }

        if (status == ChatRoomStatus.CLOSED) {
            throw new IllegalStateException("종료된 채팅방 상태는 변경할 수 없습니다.");
        }

        this.status = nextStatus;
    }
}
