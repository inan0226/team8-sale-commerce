# Project Context

이 문서는 8 team Sale Commerce 프로젝트 전용 맥락이다. 문서화, 코드 리뷰, 트러블슈팅, 의사결정 기록을 작성할 때 프로젝트 사실 확인용으로 사용한다.

## 프로젝트 개요

- 프로젝트명: team8-sale-commerce
- 목적: 한정 수량 특가 판매와 일반 상품 주문을 함께 지원하는 커머스 백엔드
- 주요 기능:
  - 일반 상품 주문: 장바구니 기반 주문 생성, 재고 차감, 주문 조회, 주문 취소
  - 선착순 특가 구매: Redis/Redisson 분산 락으로 동시 구매 요청과 재고 정합성 제어
  - 결제/환불: PortOne 결제 검증, 결제 실패 처리, 환불 보상 흐름
  - 검색/랭킹: 상품 검색 캐싱, Redis Sorted Set 기반 인기 검색어
  - 실시간 채팅: WebSocket/STOMP 기반 상담 채팅
  - 인증/권한: JWT Stateless 인증, USER/ADMIN 권한 분리

## 기술 스택

- Language: Java 21
- Framework: Spring Boot 4.1, Spring MVC, Spring Security
- Data: Spring Data JPA, MySQL, H2(Test)
- Cache/Lock: Redis, Spring Cache, Redisson
- Realtime: WebSocket, STOMP
- Authentication: JWT(JJWT), BCrypt
- Payment: PortOne V2 REST API
- Test: JUnit 5, Spring Boot Test, Testcontainers
- Build: Gradle
- Query: QueryDSL

## 실제 패키지 구조

현재 저장소 기준 루트 패키지는 다음과 같다.

```text
com.example.team8salecommerce
├── domain
│   ├── auth
│   ├── member
│   ├── product
│   ├── category
│   ├── search
│   ├── cart
│   ├── order
│   ├── promotion
│   ├── payment
│   ├── refund
│   ├── stock
│   └── chat
└── global
    ├── config
    ├── exception
    ├── response
    ├── security
    ├── util
    └── websocket
```

컨벤션 문서의 예시 패키지명과 실제 저장소 패키지명이 다르면 실제 저장소 기준인 `com.example.team8salecommerce`를 우선한다.

## 공통 응답/예외

- 모든 REST 응답은 `global.response.ApiResponse<T>`를 사용한다.
- 성공 응답 기본 메시지는 `요청이 성공했습니다.`이다.
- 예외는 `global.exception.CustomException`과 `global.exception.ErrorCode`로 관리한다.
- 대표 ErrorCode:
  - `INVALID_REQUEST`
  - `MEMBER_NOT_FOUND`
  - `UNAUTHORIZED`
  - `FORBIDDEN`
  - `PRODUCT_NOT_FOUND`
  - `CART_NOT_FOUND`
  - `CART_ITEM_NOT_FOUND`
  - `ORDER_NOT_FOUND`
  - `INVALID_ORDER_STATUS`
  - `INVALID_QUANTITY`
  - `OUT_OF_STOCK`
  - `LOCK_ACQUIRE_FAILED`
  - `PAYMENT_AMOUNT_MISMATCH`
  - `DUPLICATED_PAYMENT`
  - `REFUND_NOT_ALLOWED`

## 인증 흐름

- Controller에서는 `@AuthenticationPrincipal AuthMember authMember`를 받는다.
- 실제 memberId 추출은 `AuthMemberResolver.requireMemberId(authMember)`를 사용한다.
- `AuthMember`는 `memberId`, `email`, `role`만 담는 record다.
- 권한은 `ROLE_` prefix를 붙여 Spring Security 권한으로 변환한다.

## 주요 API

```text
POST   /auth/signup
POST   /auth/login
POST   /auth/logout
GET    /members/me
GET    /products
GET    /products/{productId}
GET    /products/search
GET    /categories/{categoryId}/products
POST   /cart/items
GET    /cart
PATCH  /cart/items/{cartItemId}
DELETE /cart/items/{cartItemId}
POST   /orders
GET    /orders
PATCH  /orders/{orderId}/cancel
POST   /promotions/{promotionProductId}/purchase
POST   /payments/confirm
POST   /payments/fail
POST   /orders/{orderId}/refunds
GET    /refunds/{refundId}
GET    /chat/rooms
POST   /chat/rooms
GET    /chat/rooms/{chatRoomId}/messages
PATCH  /chat/rooms/{chatRoomId}/status
```

## 도메인별 핵심 설계 포인트

### Product

- 삭제된 상품은 조회와 주문 대상에서 제외한다.
- 일반 상품 조회는 `findByIdAndIsDeletedFalse`, `findByIsDeletedFalse`처럼 삭제 여부를 필터링한다.
- 가격은 `Long`을 사용한다.

### Cart

- 회원별 장바구니 소유권을 검증한다.
- 장바구니는 회원가입 시점에 반드시 생성되어 있지 않을 수 있다.
- 첫 상품 추가 시 장바구니가 없으면 생성한다.
- 장바구니 조회 시 장바구니가 없으면 빈 장바구니 응답을 반환한다.
- 동일 상품을 다시 담으면 새 row를 만들지 않고 수량을 증가시킨다.
- 삭제된 CartItem은 `deletedAt` 기반 soft delete로 처리한다.
- soft deleted CartItem이 재추가되면 복구 대상으로 처리할 수 있다.

### Order

- 일반 주문은 선택된 장바구니 상품 ID 목록으로 생성한다.
- 요청 `cartItemIds`는 비어 있거나 null이면 안 되고, 중복과 0 이하 값을 거부한다.
- 주문 대상 CartItem은 활성 항목만 조회한다.
- 재고 차감은 `OrderProductRepository.decreaseStock(productId, quantity)`의 조건부 UPDATE로 처리한다.
- 삭제 상품은 주문 대상에서 제외한다.
- 주문 생성 시 OrderItem에는 상품명/상품가격 스냅샷을 저장한다.
- 주문 완료 후 대상 CartItem은 soft delete 처리한다.
- 주문 취소는 `WAITING` 상태에서만 가능하고 재고를 복구한다.
- 주문 상태는 `WAITING`, `PAID`, `PAYMENT_FAILED`, `CANCELLED`, `REFUND_REQUEST`, `REFUNDED` 등을 사용한다.

### Promotion / Payment / Refund

- 특가 구매는 `promotionProductId` 단위 Redisson 분산 락을 사용한다.
- 락 획득 후 판매 시간, 상태, 재고를 다시 검증한다.
- 결제는 클라이언트 결과를 그대로 믿지 않고 PortOne에서 상태와 금액을 재조회한다.
- 외부 PortOne 호출과 내부 DB 상태 변경은 트랜잭션 경계를 분리한다.
- 환불은 외부 환불 성공과 내부 완료 처리 사이의 중간 상태를 남겨 복구 가능성을 확보한다.
