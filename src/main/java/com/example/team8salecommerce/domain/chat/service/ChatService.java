package com.example.team8salecommerce.domain.chat.service;

import com.example.team8salecommerce.domain.chat.dto.ChatMessageRequest;
import com.example.team8salecommerce.domain.chat.dto.ChatMessageResponse;
import com.example.team8salecommerce.domain.chat.dto.ChatRoomResponse;
import com.example.team8salecommerce.domain.chat.dto.CreateChatRoomRequest;
import com.example.team8salecommerce.domain.chat.entity.ChatMessage;
import com.example.team8salecommerce.domain.chat.entity.ChatRoom;
import com.example.team8salecommerce.domain.chat.repository.ChatMessageRepository;
import com.example.team8salecommerce.domain.chat.repository.ChatRoomRepository;
import com.example.team8salecommerce.domain.member.entity.Member;
import com.example.team8salecommerce.domain.member.repository.MemberRepository;
import com.example.team8salecommerce.global.exception.CustomException;
import com.example.team8salecommerce.global.exception.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public ChatRoomResponse createRoom(Long memberId, CreateChatRoomRequest request) {
        Member member = findMember(memberId);
        ChatRoom chatRoom = chatRoomRepository.save(ChatRoom.create(request.name(), member));

        return ChatRoomResponse.from(chatRoom);
    }

    public List<ChatRoomResponse> getMyRooms(Long memberId) {
        return chatRoomRepository.findAllByCreatedByIdOrderByCreatedAtDesc(memberId)
                .stream()
                .map(ChatRoomResponse::from)
                .toList();
    }

    public List<ChatMessageResponse> getMessages(Long roomId) {
        validateRoom(roomId);

        return chatMessageRepository.findAllByChatRoomIdOrderByCreatedAtAsc(roomId)
                .stream()
                .map(ChatMessageResponse::from)
                .toList();
    }

    @Transactional
    public ChatMessageResponse sendMessage(Long roomId, Long memberId, ChatMessageRequest request) {
        if (!StringUtils.hasText(request.content())) {
            throw new CustomException(ErrorCode.CHAT_MESSAGE_EMPTY);
        }

        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND));
        Member sender = findMember(memberId);
        ChatMessage chatMessage = chatMessageRepository.save(
                ChatMessage.create(chatRoom, sender, request.content())
        );

        return ChatMessageResponse.from(chatMessage);
    }

    public void validateRoom(Long roomId) {
        if (!chatRoomRepository.existsById(roomId)) {
            throw new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND);
        }
    }

    private Member findMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
