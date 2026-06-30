package com.example.team8salecommerce.domain.chat.service;

import com.example.team8salecommerce.domain.chat.dto.ChatMessageRequest;
import com.example.team8salecommerce.domain.chat.dto.ChatMessageResponse;
import com.example.team8salecommerce.domain.chat.dto.ChatRoomResponse;
import com.example.team8salecommerce.domain.chat.dto.CreateChatRoomRequest;
import com.example.team8salecommerce.domain.chat.entity.ChatMessage;
import com.example.team8salecommerce.domain.chat.entity.ChatRoom;
import com.example.team8salecommerce.domain.chat.entity.ChatRoomStatus;
import com.example.team8salecommerce.domain.chat.repository.ChatMessageRepository;
import com.example.team8salecommerce.domain.chat.repository.ChatRoomRepository;
import com.example.team8salecommerce.domain.member.entity.Member;
import com.example.team8salecommerce.domain.member.entity.Role;
import com.example.team8salecommerce.domain.member.repository.MemberRepository;
import com.example.team8salecommerce.global.exception.CustomException;
import com.example.team8salecommerce.global.exception.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 채팅방과 채팅 메시지의 비즈니스 로직을 담당하는 서비스입니다.
 *
 * <p>채팅 인증/인가는 컨트롤러나 WebSocket 인터셉터에서 끝나는 것이 아니라,
 * 실제 데이터에 접근하기 직전에 이 서비스에서도 한 번 더 확인합니다.
 * 예를 들어 다른 사용자의 채팅방 메시지를 조회하거나 전송하려는 요청은 여기서 차단됩니다.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final MemberRepository memberRepository;

    /**
     * 로그인한 회원의 채팅방을 생성합니다.
     *
     * <p>이미 닫히지 않은 채팅방이 있으면 새로 만들지 않고 기존 방을 반환합니다.
     * 이렇게 하면 한 사용자가 동시에 여러 개의 상담방을 만드는 상황을 줄일 수 있습니다.</p>
     */
    @Transactional
    public ChatRoomResponse createRoom(Long memberId, CreateChatRoomRequest request) {
        Member member = memberRepository.findByIdForUpdate(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        ChatRoom existingRoom = chatRoomRepository
                .findFirstByCreatedByIdAndStatusNotOrderByCreatedAtDesc(memberId, ChatRoomStatus.CLOSED)
                .orElse(null);
        if (existingRoom != null) {
            return ChatRoomResponse.from(existingRoom);
        }

        ChatRoom chatRoom = chatRoomRepository.save(ChatRoom.create(request.name(), member));

        return ChatRoomResponse.from(chatRoom);
    }

    /**
     * 회원 역할에 따라 조회할 채팅방 목록을 결정합니다.
     *
     * <p>관리자라면 전체 채팅방을 보고, 일반 사용자라면 본인이 만든 채팅방만 봅니다.
     * 이 메서드는 "누가 어떤 목록을 볼 수 있는지"를 결정하는 인가 지점입니다.</p>
     */
    public List<ChatRoomResponse> getRooms(Long memberId, Role role) {
        List<ChatRoom> chatRooms = role == Role.ADMIN
                ? chatRoomRepository.findAllByOrderByCreatedAtDesc()
                : chatRoomRepository.findAllByCreatedByIdOrderByCreatedAtDesc(memberId);

        return chatRooms
                .stream()
                .map(ChatRoomResponse::from)
                .toList();
    }

    /**
     * 특정 채팅방의 메시지를 조회합니다.
     *
     * <p>메시지를 가져오기 전에 {@link #validateRoomAccess(Long, Role, Long)}를 호출해
     * 현재 사용자가 해당 채팅방을 볼 수 있는지 먼저 확인합니다.</p>
     */
    public List<ChatMessageResponse> getMessages(Long memberId, Role role, Long roomId) {
        validateRoomAccess(memberId, role, roomId);

        return chatMessageRepository.findAllByChatRoomIdOrderByCreatedAtAsc(roomId)
                .stream()
                .map(ChatMessageResponse::from)
                .toList();
    }

    /**
     * 채팅 메시지를 저장합니다.
     *
     * <p>WebSocket 컨트롤러에서 로그인 회원 ID를 넘겨주면,
     * 이 메서드는 해당 회원이 방에 접근할 수 있는지 확인하고,
     * 닫힌 채팅방에는 메시지를 보낼 수 없도록 차단합니다.</p>
     */
    @Transactional
    public ChatMessageResponse sendMessage(Long roomId, Long memberId, Role role, ChatMessageRequest request) {
        if (!StringUtils.hasText(request.content())) {
            throw new CustomException(ErrorCode.CHAT_MESSAGE_EMPTY);
        }

        ChatRoom chatRoom = findAccessibleRoom(memberId, role, roomId);
        if (chatRoom.isClosed()) {
            throw new CustomException(ErrorCode.CHAT_ROOM_CLOSED);
        }

        Member sender = findMember(memberId);
        ChatMessage chatMessage = chatMessageRepository.save(
                ChatMessage.create(chatRoom, sender, request.content())
        );

        return ChatMessageResponse.from(chatMessage);
    }

    /**
     * 채팅방이 실제로 존재하는지만 확인합니다.
     */
    public void validateRoom(Long roomId) {
        if (!chatRoomRepository.existsById(roomId)) {
            throw new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND);
        }
    }

    /**
     * 현재 회원이 특정 채팅방에 접근할 수 있는지 확인합니다.
     *
     * <p>관리자는 모든 방에 접근할 수 있고, 일반 사용자는 자신이 만든 방에만 접근할 수 있습니다.
     * WebSocket 구독 인가와 HTTP 메시지 조회 인가가 모두 이 메서드를 사용합니다.</p>
     */
    public void validateRoomAccess(Long memberId, Role role, Long roomId) {
        findAccessibleRoom(memberId, role, roomId);
    }

    /**
     * 채팅방 상태를 변경합니다.
     *
     * <p>관리자 권한 확인은 컨트롤러에서 먼저 수행하고,
     * 이 메서드는 실제 상태 전환 규칙을 적용합니다.</p>
     */
    @Transactional
    public ChatRoomResponse updateRoomStatus(Long roomId, ChatRoomStatus status) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        try {
            chatRoom.updateStatus(status);
        } catch (IllegalStateException exception) {
            throw new CustomException(ErrorCode.INVALID_CHAT_ROOM_STATUS);
        }

        return ChatRoomResponse.from(chatRoom);
    }

    /**
     * 채팅방을 조회하면서 접근 권한까지 함께 확인합니다.
     */
    private ChatRoom findAccessibleRoom(Long memberId, Role role, Long roomId) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        if (role != Role.ADMIN && !chatRoom.getCreatedBy().getId().equals(memberId)) {
            throw new CustomException(ErrorCode.CHAT_ACCESS_DENIED);
        }

        return chatRoom;
    }

    /**
     * 메시지 발신자로 사용할 회원 엔티티를 조회합니다.
     */
    private Member findMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
