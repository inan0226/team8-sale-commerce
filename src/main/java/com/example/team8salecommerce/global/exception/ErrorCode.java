package com.example.team8salecommerce.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),

    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."),
    DUPLICATED_EMAIL(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    DUPLICATED_NICKNAME(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다."),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "비밀번호가 일치하지 않습니다."),

    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다."),
    AUTH_TOKEN_STORE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "인증 저장소에 연결할 수 없습니다."),

    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "카테고리를 찾을 수 없습니다."),
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다."),
    CART_NOT_FOUND(HttpStatus.NOT_FOUND, "장바구니를 찾을 수 없습니다."),
    CART_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "장바구니 상품을 찾을 수 없습니다."),

    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다."),
    INVALID_ORDER_STATUS(HttpStatus.BAD_REQUEST, "주문 상태가 올바르지 않습니다."),

	PROMOTION_PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "특가 상품을 찾을 수 없습니다."),
	PROMOTION_ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "특가 주문을 찾을 수 없습니다."),
	PROMOTION_ORDER_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "특가 주문 상품을 찾을 수 없습니다."),
	PROMOTION_NOT_OPEN(HttpStatus.BAD_REQUEST, "특가 이벤트가 진행 중이 아닙니다."),
	INVALID_QUANTITY(HttpStatus.BAD_REQUEST, "구매 수량이 올바르지 않습니다."),
	INVALID_PROMOTION_ORDER_STATUS(HttpStatus.BAD_REQUEST, "특가 주문 상태가 올바르지 않습니다."),
	OUT_OF_STOCK(HttpStatus.CONFLICT, "재고가 부족합니다."),
	LOCK_ACQUIRE_FAILED(HttpStatus.CONFLICT, "구매 요청이 몰려 처리하지 못했습니다. 다시 시도해주세요."),

	PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "결제 정보를 찾을 수 없습니다."),
	PAYMENT_AMOUNT_MISMATCH(HttpStatus.BAD_REQUEST, "결제 금액이 일치하지 않습니다."),
	PAYMENT_ALREADY_COMPLETED(HttpStatus.CONFLICT, "이미 결제 완료된 주문입니다."),
	PAYMENT_ALREADY_FAILED(HttpStatus.CONFLICT, "이미 결제 실패 처리된 주문입니다."),
	PAYMENT_CONFIRM_FAILED(HttpStatus.BAD_REQUEST, "결제 승인에 실패했습니다."),
	PAYMENT_FAIL_FAILED(HttpStatus.BAD_REQUEST, "결제 실패 처리에 실패했습니다."),

	REFUND_NOT_FOUND(HttpStatus.NOT_FOUND, "환불 정보를 찾을 수 없습니다."),
	REFUND_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "환불할 수 없는 주문 상태입니다."),
	REFUND_ALREADY_REQUESTED(HttpStatus.CONFLICT, "이미 환불 요청된 주문입니다."),
	ALREADY_REFUNDED(HttpStatus.CONFLICT, "이미 환불 완료된 주문입니다."),
	REFUND_FAILED(HttpStatus.BAD_REQUEST, "환불 처리에 실패했습니다."),

    STOCK_HISTORY_NOT_FOUND(HttpStatus.NOT_FOUND, "재고 이력을 찾을 수 없습니다."),

    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "채팅방을 찾을 수 없습니다."),
    CHAT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "해당 채팅방에 접근할 수 없습니다."),
    CHAT_MESSAGE_EMPTY(HttpStatus.BAD_REQUEST, "메시지 내용이 비어 있습니다."),
    INVALID_CHAT_ROOM_STATUS(HttpStatus.BAD_REQUEST, "채팅방 상태가 올바르지 않습니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
