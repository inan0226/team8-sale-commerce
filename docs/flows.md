# 오늘의 특가 마켓 플로우차트

이 문서는 주요 API와 도메인 흐름을 Mermaid 플로우차트로 정리한 문서입니다. 기능 구현, PR 리뷰, 테스트 케이스 작성 시 참고합니다.

## 인증 흐름

```mermaid
flowchart TD
    signup["POST /auth/signup"] --> dupEmail["이메일 중복 확인"]
    dupEmail --> dupNickname["닉네임 중복 확인"]
    dupNickname --> hash["비밀번호 해싱"]
    hash --> saveMember["MEMBER 저장"]

    login["POST /auth/login"] --> findMember["이메일로 회원 조회"]
    findMember --> validatePassword["비밀번호 검증"]
    validatePassword --> issueJwt["Access/Refresh Token 발급"]
    issueJwt --> saveRefresh["Redis에 Refresh Token 저장"]

    logout["POST /auth/logout"] --> validateToken["Access Token 검증"]
    validateToken --> blacklist["Redis에 Access Token blacklist 저장"]
    blacklist --> deleteRefresh["Refresh Token 삭제"]
```

검토 포인트:
- 비밀번호 원문 저장 금지
- 로그아웃 시 access token blacklist와 refresh token 삭제 모두 처리
- Redis가 필요한 테스트/프로필 분리 여부 확인

## 상품 검색 및 상세 조회 흐름

```mermaid
flowchart TD
    search["GET /products/search"] --> normalize["검색 조건 정규화"]
    normalize --> cacheKey["Caffeine cache key 생성"]
    cacheKey --> query["삭제되지 않은 상품 조회"]
    query --> popular["검색어가 있으면 Redis 인기 검색어 증가"]
    popular --> response["페이지 응답 반환"]

    detail["GET /products/{productId}"] --> findProduct["삭제되지 않은 상품 조회"]
    findProduct --> notFound{"상품 존재?"}
    notFound -- no --> product404["PRODUCT_NOT_FOUND"]
    notFound -- yes --> detailResponse["상세 DTO 반환"]
```

검토 포인트:
- 삭제 상품 노출 금지
- 가격 범위와 빈 검색어 처리 확인
- 검색 조건이 cache key에 모두 포함되는지 확인

## 장바구니 흐름

```mermaid
flowchart TD
    add["POST /cart/items"] --> auth["인증 회원 확인"]
    auth --> getCart["회원 장바구니 조회 또는 생성"]
    getCart --> findProduct["삭제되지 않은 상품 조회"]
    findProduct --> activeItem{"활성 장바구니 상품 존재?"}
    activeItem -- yes --> increase["수량 증가"]
    activeItem -- no --> createItem["장바구니 상품 생성"]
    increase --> itemResponse["상품 DTO 반환"]
    createItem --> itemResponse

    list["GET /cart"] --> findMemberCart["회원 장바구니 조회"]
    findMemberCart --> empty{"장바구니 존재?"}
    empty -- no --> emptyResponse["빈 장바구니 반환"]
    empty -- yes --> activeItems["삭제되지 않은 장바구니 상품 조회"]
    activeItems --> total["총 금액 계산"]
    total --> cartResponse["장바구니 DTO 반환"]

    update["PATCH /cart/items/{cartItemId}"] --> validateOwner["회원 장바구니 상품인지 확인"]
    validateOwner --> validateQuantity["수량 1 이상 검증"]
    validateQuantity --> updateQuantity["수량 변경"]

    delete["DELETE /cart/items/{cartItemId}"] --> validateOwnerDelete["회원 장바구니 상품인지 확인"]
    validateOwnerDelete --> softDelete["deletedAt 저장"]
```

검토 포인트:
- 컨트롤러에서 인증 객체 null 처리
- soft delete와 unique 제약 조합에서 삭제 상품 재담기 가능 여부 확인
- 수량은 1 이상이어야 함
- 수정/삭제 시 타 회원 장바구니 상품 접근 차단

## 특가 구매 흐름

```mermaid
flowchart TD
    purchase["POST /promotions/{promotionProductId}/purchase"] --> lock["promotionProductId 기준 Redis Lock 획득"]
    lock --> lockOk{"Lock 획득 성공?"}
    lockOk -- no --> lockFail["구매 실패 반환"]
    lockOk -- yes --> reread["특가 상품 재조회"]
    reread --> validateTime["이벤트 시간과 OPEN 상태 검증"]
    validateTime --> enoughStock{"이벤트 재고 충분?"}
    enoughStock -- no --> soldOut["SOLD_OUT 처리 및 품절 안내 반환"]
    enoughStock -- yes --> decrease["이벤트 재고 차감"]
    decrease --> createOrder["PROMOTION_ORDER WAITING 생성"]
    createOrder --> createItem["PROMOTION_ORDER_ITEM 스냅샷 생성"]
    createItem --> stockHistory["DECREASE 재고 이력 생성"]
    stockHistory --> paymentPage["결제 페이지 이동 데이터 반환"]
    soldOut --> release["Redis Lock 해제"]
    paymentPage --> release
```

검토 포인트:
- 모든 분기에서 Redis Lock 해제
- Lock 획득 후 mutable 데이터 재조회
- 재고 차감 전후 수량과 이력 정확성
- 품절 상태와 UI 안내 일관성

## 결제 승인 흐름

```mermaid
flowchart TD
    confirm["POST /payments/confirm"] --> lockOrder["주문 row lock 조회"]
    lockOrder --> owner["주문 소유자 확인"]
    owner --> waiting{"주문 WAITING?"}
    waiting -- no --> invalidStatus["상태 오류 반환"]
    waiting -- yes --> portone["PortOne 결제 조회"]
    portone --> amount{"금액 일치?"}
    amount -- no --> saveFail["FAILED 결제 저장"]
    amount -- yes --> duplicate["PortOne 결제 ID 중복 확인"]
    duplicate --> savePaid["PAID 결제 저장"]
    savePaid --> markPaid["주문 PAID 변경"]
    saveFail --> restoreStock["이벤트 재고 1회 복구"]
    restoreStock --> markFailed["주문 PAYMENT_FAILED 변경"]
```

검토 포인트:
- 주문 소유자와 주문 상태를 먼저 확인
- 결제 금액 위변조 방지
- PortOne 결제 ID 중복 방지
- 실패 처리 시 재고 복구가 정확히 한 번만 발생

## 환불 흐름

```mermaid
flowchart TD
    refund["POST /orders/{orderId}/refunds"] --> requestTx["트랜잭션 1: REFUND_REQUEST 생성 및 주문 REFUND_REQUEST 변경"]
    requestTx --> portone["DB row lock 밖에서 PortOne 환불 호출"]
    portone --> success{"PortOne 환불 성공?"}
    success -- no --> failTx["환불 REFUND_FAILED 및 주문 PAID 복구"]
    success -- yes --> recordTx["cancellationId/status 저장 및 PORTONE_REFUND_SUCCEEDED 변경"]
    recordTx --> completeTx["재고 복구, 재고 이력 저장, 환불/주문 REFUNDED 변경"]
    completeTx --> response["환불 완료 응답 반환"]
```

검토 포인트:
- DB row lock을 잡은 상태로 PortOne을 호출하지 않기
- PortOne 성공 정보는 내부 완료 처리보다 먼저 저장
- PortOne 성공 후 내부 완료 실패 시 운영 복구 가능한 데이터가 남아야 함
- 주문 소유자와 `PAID` 상태 검증
- 중복 환불 요청 차단
- 재고 복구가 정확히 한 번만 발생

## 채팅 흐름

```mermaid
flowchart TD
    createRoom["POST /chat/rooms"] --> activeRoom{"회원의 non-CLOSED 방 존재?"}
    activeRoom -- yes --> returnRoom["기존 활성 방 반환"]
    activeRoom -- no --> createWaiting["WAITING 방 생성"]

    listRooms["GET /chat/rooms"] --> role{"ADMIN?"}
    role -- yes --> allRooms["전체 방 반환"]
    role -- no --> ownRooms["내 방만 반환"]

    messages["GET /chat/rooms/{roomId}/messages"] --> access["소유자 또는 관리자 접근 확인"]
    access --> returnMessages["생성순 메시지 반환"]

    connect["STOMP CONNECT"] --> jwt["JWT 검증"]
    jwt --> principal["AuthMember principal 설정"]
    subscribe["SUBSCRIBE /sub/chat/rooms/{roomId}"] --> subAccess["방 접근 권한 확인"]
    send["SEND /pub/chat/rooms/{roomId}/messages"] --> sendAccess["발신자 방 참여 권한 확인"]
    sendAccess --> closed{"방 CLOSED?"}
    closed -- yes --> reject["메시지 거절"]
    closed -- no --> persist["메시지 저장"]
    persist --> broadcast["/sub/chat/rooms/{roomId} broadcast"]

    status["PATCH /chat/rooms/{roomId}/status"] --> admin["ADMIN 권한 확인"]
    admin --> transition["상태 전이 검증"]
    transition --> update["방 상태 변경"]
```

검토 포인트:
- WebSocket subscribe/send 경로 일치
- 잘못된 `/sub/chat/**` destination 차단
- 메시지 저장 후 broadcast
- `CLOSED` 방 메시지 전송 차단
- 관리자 메시지 조회는 role 기반으로 처리
