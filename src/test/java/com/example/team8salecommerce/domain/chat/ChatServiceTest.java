package com.example.team8salecommerce.domain.chat;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.team8salecommerce.domain.chat.dto.ChatRoomResponse;
import com.example.team8salecommerce.domain.chat.dto.ChatMessageRequest;
import com.example.team8salecommerce.domain.chat.dto.CreateChatRoomRequest;
import com.example.team8salecommerce.domain.chat.entity.ChatMessage;
import com.example.team8salecommerce.domain.chat.entity.ChatRoom;
import com.example.team8salecommerce.domain.chat.entity.ChatRoomStatus;
import com.example.team8salecommerce.domain.chat.repository.ChatMessageRepository;
import com.example.team8salecommerce.domain.chat.repository.ChatRoomRepository;
import com.example.team8salecommerce.domain.chat.service.ChatService;
import com.example.team8salecommerce.domain.member.entity.Member;
import com.example.team8salecommerce.domain.member.entity.Role;
import com.example.team8salecommerce.domain.member.repository.MemberRepository;
import com.example.team8salecommerce.global.exception.CustomException;
import com.example.team8salecommerce.global.exception.ErrorCode;
import java.util.List;
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
    void createRoom_returnsExistingActiveRoom() {
        Long memberId = 1L;
        Member member = member(memberId);
        ChatRoom existingRoom = ChatRoom.create("general", member);

        when(memberRepository.findByIdForUpdate(memberId)).thenReturn(Optional.of(member));
        when(chatRoomRepository.findFirstByCreatedByIdAndStatusNotOrderByCreatedAtDesc(
                memberId,
                ChatRoomStatus.CLOSED
        )).thenReturn(Optional.of(existingRoom));

        ChatRoomResponse response = chatService.createRoom(memberId, new CreateChatRoomRequest("new room"));

        assertThat(response.name()).isEqualTo("general");
        assertThat(response.status()).isEqualTo(ChatRoomStatus.WAITING);
        verify(chatRoomRepository, never()).save(any(ChatRoom.class));
    }

    @Test
    void getRooms_adminReturnsAllRooms() {
        ChatRoom firstRoom = ChatRoom.create("first", member(1L));
        ChatRoom secondRoom = ChatRoom.create("second", member(2L));

        when(chatRoomRepository.findAllByOrderByCreatedAtDesc())
                .thenReturn(List.of(firstRoom, secondRoom));

        List<ChatRoomResponse> responses = chatService.getRooms(99L, Role.ADMIN);

        assertThat(responses).hasSize(2);
        verify(chatRoomRepository, never()).findAllByCreatedByIdOrderByCreatedAtDesc(any());
    }

    @Test
    void getMessages_deniesOtherMembersRoom() {
        Long ownerId = 1L;
        Long otherMemberId = 2L;
        Long roomId = 10L;
        ChatRoom chatRoom = ChatRoom.create("general", member(ownerId));

        when(chatRoomRepository.findById(roomId))
                .thenReturn(Optional.of(chatRoom));

        assertThatThrownBy(() -> chatService.getMessages(otherMemberId, Role.USER, roomId))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.CHAT_ACCESS_DENIED.getMessage());

        verify(chatMessageRepository, never()).findAllByChatRoomIdOrderByCreatedAtAsc(any());
    }

    @Test
    void getMessages_adminCanReadMembersRoom() {
        Long ownerId = 1L;
        Long adminId = 99L;
        Long roomId = 10L;
        ChatRoom chatRoom = ChatRoom.create("general", member(ownerId));

        when(chatRoomRepository.findById(roomId))
                .thenReturn(Optional.of(chatRoom));
        when(chatMessageRepository.findAllByChatRoomIdOrderByCreatedAtAsc(roomId))
                .thenReturn(List.of());

        assertThat(chatService.getMessages(adminId, Role.ADMIN, roomId)).isEmpty();
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

        assertThatThrownBy(() -> chatService.sendMessage(roomId, otherMemberId, Role.USER, request))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.CHAT_ACCESS_DENIED.getMessage());

        verify(chatMessageRepository, never()).save(any(ChatMessage.class));
    }

    @Test
    void sendMessage_adminCanSendToMembersRoom() {
        Long ownerId = 1L;
        Long adminId = 99L;
        Long roomId = 10L;
        ChatRoom chatRoom = ChatRoom.create("general", member(ownerId));
        Member admin = member(adminId);
        ChatMessageRequest request = new ChatMessageRequest(roomId, "hello");

        when(chatRoomRepository.findById(roomId))
                .thenReturn(Optional.of(chatRoom));
        when(memberRepository.findById(adminId))
                .thenReturn(Optional.of(admin));
        when(chatMessageRepository.save(any(ChatMessage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(chatService.sendMessage(roomId, adminId, Role.ADMIN, request).senderId())
                .isEqualTo(adminId);
    }

    @Test
    void sendMessage_deniesClosedRoom() {
        Long ownerId = 1L;
        Long roomId = 10L;
        ChatRoom chatRoom = ChatRoom.create("general", member(ownerId));
        chatRoom.updateStatus(ChatRoomStatus.CLOSED);
        ChatMessageRequest request = new ChatMessageRequest(roomId, "hello");

        when(chatRoomRepository.findById(roomId))
                .thenReturn(Optional.of(chatRoom));

        assertThatThrownBy(() -> chatService.sendMessage(roomId, ownerId, Role.USER, request))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.CHAT_ROOM_CLOSED.getMessage());

        verify(chatMessageRepository, never()).save(any(ChatMessage.class));
    }

    @Test
    void updateRoomStatus_updatesStatus() {
        Long ownerId = 1L;
        Long roomId = 10L;
        ChatRoom chatRoom = ChatRoom.create("general", member(ownerId));

        when(chatRoomRepository.findById(roomId))
                .thenReturn(Optional.of(chatRoom));

        ChatRoomResponse response = chatService.updateRoomStatus(roomId, ChatRoomStatus.IN_PROGRESS);

        assertThat(response.status()).isEqualTo(ChatRoomStatus.IN_PROGRESS);
    }

    private Member member(Long memberId) {
        Member member = mock(Member.class);
        lenient().when(member.getId()).thenReturn(memberId);
        lenient().when(member.getNickname()).thenReturn("member-" + memberId);
        return member;
    }
}
