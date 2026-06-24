package com.example.team8salecommerce.domain.chat.controller;

import com.example.team8salecommerce.domain.chat.dto.ChatMessageResponse;
import com.example.team8salecommerce.domain.chat.dto.ChatRoomResponse;
import com.example.team8salecommerce.domain.chat.dto.CreateChatRoomRequest;
import com.example.team8salecommerce.domain.chat.service.ChatService;
import com.example.team8salecommerce.global.response.ApiResponse;
import com.example.team8salecommerce.global.security.AuthMember;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/chat/rooms")
public class ChatRoomController {

    private final ChatService chatService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ChatRoomResponse>>> getMyRooms(
            @AuthenticationPrincipal AuthMember authMember
    ) {
        return ResponseEntity.ok(ApiResponse.success(chatService.getMyRooms(authMember.memberId())));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ChatRoomResponse>> createRoom(
            @AuthenticationPrincipal AuthMember authMember,
            @Valid @RequestBody CreateChatRoomRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(chatService.createRoom(authMember.memberId(), request)));
    }

    @GetMapping("/{chatRoomId}/messages")
    public ResponseEntity<ApiResponse<List<ChatMessageResponse>>> getMessages(
            @PathVariable Long chatRoomId
    ) {
        return ResponseEntity.ok(ApiResponse.success(chatService.getMessages(chatRoomId)));
    }
}
