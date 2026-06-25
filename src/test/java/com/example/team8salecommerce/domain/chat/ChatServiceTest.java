package com.example.team8salecommerce.domain.chat;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.team8salecommerce.domain.chat.dto.ChatMessageRequest;
import com.example.team8salecommerce.domain.chat.entity.ChatMessage;
import com.example.team8salecommerce.domain.chat.entity.ChatRoom;
import com.example.team8salecommerce.domain.chat.repository.ChatMessageRepository;
import com.example.team8salecommerce.domain.chat.repository.ChatRoomRepository;
import com.example.team8salecommerce.domain.chat.service.ChatService;
import com.example.team8salecommerce.domain.member.entity.Member;
import com.example.team8salecommerce.domain.member.repository.MemberRepository;
import com.example.team8salecommerce.global.exception.CustomException;
import com.example.team8salecommerce.global.exception.ErrorCode;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private ChatService chatService;

    @Test
    void getMessages_deniesOtherMembersRoom() {
        Long ownerId = 1L;
        Long otherMemberId = 2L;
        Long roomId = 10L;
        ChatRoom chatRoom = ChatRoom.create("general", member(ownerId));

        when(chatRoomRepository.findById(roomId))
                .thenReturn(Optional.of(chatRoom));

        assertThatThrownBy(() -> chatService.getMessages(otherMemberId, roomId))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.CHAT_ACCESS_DENIED.getMessage());

        verify(chatMessageRepository, never()).findAllByChatRoomIdOrderByCreatedAtAsc(any());
    }

    @Test
    void sendMessage_deniesOtherMembersRoom() {
        Long ownerId = 1L;
        Long otherMemberId = 2L;
        Long roomId = 10L;
        ChatRoom chatRoom = ChatRoom.create("general", member(ownerId));
        ChatMessageRequest request = new ChatMessageRequest(roomId, "hello");

        when(chatRoomRepository.findById(roomId))
                .thenReturn(Optional.of(chatRoom));

        assertThatThrownBy(() -> chatService.sendMessage(roomId, otherMemberId, request))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.CHAT_ACCESS_DENIED.getMessage());

        verify(chatMessageRepository, never()).save(any(ChatMessage.class));
    }

    private Member member(Long memberId) {
        Member member = mock(Member.class);
        when(member.getId()).thenReturn(memberId);
        return member;
    }
}
