package com.example.team8salecommerce.domain.chat.controller;

import com.example.team8salecommerce.domain.chat.dto.ChatMessageResponse;
import com.example.team8salecommerce.domain.chat.dto.ChatRoomResponse;
import com.example.team8salecommerce.domain.chat.dto.CreateChatRoomRequest;
import com.example.team8salecommerce.domain.chat.dto.UpdateChatRoomStatusRequest;
import com.example.team8salecommerce.domain.chat.service.ChatService;
import com.example.team8salecommerce.domain.member.entity.Role;
import com.example.team8salecommerce.global.response.ApiResponse;
import com.example.team8salecommerce.global.security.AuthMember;
import com.example.team8salecommerce.global.security.AuthMemberResolver;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 채팅방 생성, 조회, 메시지 조회, 상태 변경 REST API를 처리하는 컨트롤러입니다.
 *
 * <p>이 컨트롤러는 HTTP API이므로 JWT 인증은 {@link com.example.team8salecommerce.global.security.JwtAuthenticationFilter}
 * 에서 먼저 처리됩니다. 필터가 인증에 성공하면 {@code @AuthenticationPrincipal AuthMember}로
 * 현재 로그인한 회원 정보를 받을 수 있습니다.</p>
 *
 * <p>채팅방 목록/메시지 조회는 일반 사용자와 관리자의 접근 범위가 다르므로,
 * 서비스 계층에 회원 ID와 역할을 함께 넘겨 인가 검사를 수행합니다.</p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/chat/rooms")
public class ChatRoomController {

    private final ChatService chatService;
    private final AuthMemberResolver authMemberResolver;

    /**
     * 내 채팅방 목록을 조회합니다.
     *
     * <p>일반 사용자는 자신이 만든 채팅방만 보고, 관리자는 모든 채팅방을 볼 수 있습니다.
     * 이 차이는 {@link ChatService#getRooms(Long, Role)}에서 역할을 기준으로 처리합니다.</p>
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ChatRoomResponse>>> getMyRooms(
            @AuthenticationPrincipal AuthMember authMember
    ) {
        AuthMember authenticatedMember = authMemberResolver.require(authMember);

        return ResponseEntity.ok(ApiResponse.success(
                chatService.getRooms(authenticatedMember.memberId(), authenticatedMember.role())
        ));
    }

    /**
     * 새 채팅방을 생성합니다.
     *
     * <p>로그인한 회원만 채팅방을 만들 수 있으므로, 먼저 {@link AuthMemberResolver#require(AuthMember)}
     * 로 인증 회원이 존재하는지 확인합니다.</p>
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ChatRoomResponse>> createRoom(
            @AuthenticationPrincipal AuthMember authMember,
            @Valid @RequestBody CreateChatRoomRequest request
    ) {
        Long memberId = authMemberResolver.requireMemberId(authMember);

        return ResponseEntity.ok(ApiResponse.success(
                chatService.createRoom(memberId, request)
        ));
    }

    /**
     * 특정 채팅방의 메시지 목록을 조회합니다.
     *
     * <p>일반 사용자는 자신이 만든 채팅방 메시지만 볼 수 있고, 관리자는 모든 채팅방 메시지를 볼 수 있습니다.
     * 실제 접근 권한 검사는 {@link ChatService#getMessages(Long, Role, Long)}에서 수행합니다.</p>
     */
    @GetMapping("/{chatRoomId}/messages")
    public ResponseEntity<ApiResponse<List<ChatMessageResponse>>> getMessages(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long chatRoomId
    ) {
        AuthMember authenticatedMember = authMemberResolver.require(authMember);

        return ResponseEntity.ok(ApiResponse.success(
                chatService.getMessages(authenticatedMember.memberId(), authenticatedMember.role(), chatRoomId)
        ));
    }

    /**
     * 채팅방 상태를 변경합니다.
     *
     * <p>상태 변경은 상담 처리 흐름을 바꾸는 관리자 기능이므로 ADMIN 권한이 필요합니다.
     * 그래서 서비스 호출 전에 {@link AuthMemberResolver#requireRole(AuthMember, Role)}로 관리자 권한을 확인합니다.</p>
     */
    @PatchMapping("/{chatRoomId}/status")
    public ResponseEntity<ApiResponse<ChatRoomResponse>> updateStatus(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long chatRoomId,
            @Valid @RequestBody UpdateChatRoomStatusRequest request
    ) {
        // 채팅방 상태 변경은 관리자만 수행할 수 있습니다.
        authMemberResolver.requireRole(authMember, Role.ADMIN);

        return ResponseEntity.ok(ApiResponse.success(chatService.updateRoomStatus(chatRoomId, request.status())));
    }
}
