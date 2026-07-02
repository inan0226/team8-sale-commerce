# Cart / Order Checklist

Cart와 Order 도메인 관련 문서화, 코드 리뷰, 트러블슈팅, 의사결정 기록을 작성할 때 사용한다.

## Cart 체크리스트

### Controller

- [ ] URL은 `/cart`, `/cart/items`, `/cart/items/{cartItemId}` 규칙을 따른다.
- [ ] 응답은 `ResponseEntity<ApiResponse<...>>` 형태다.
- [ ] 인증 정보는 `@AuthenticationPrincipal AuthMember`로 받는다.
- [ ] `AuthMemberResolver.requireMemberId(authMember)`로 memberId를 얻는다.
- [ ] Controller에 장바구니 생성/수량 계산/소유권 검증 같은 비즈니스 로직을 넣지 않는다.

### Request / Response DTO

- [ ] DTO는 record로 작성한다.
- [ ] `AddCartItemRequest.productId`는 `@NotNull`, `@Min(1)`을 사용한다.
- [ ] `quantity`는 `@NotNull`, `@Min(1)`을 사용한다.
- [ ] 응답에는 Entity 대신 필요한 필드만 담는다.
- [ ] 가격과 총액은 `Long`으로 다룬다.

### Service

- [ ] 장바구니가 없을 때 첫 상품 추가 요청이면 Cart를 생성한다.
- [ ] 장바구니 조회 시 Cart가 없으면 빈 응답을 반환한다.
- [ ] 상품 추가 시 삭제되지 않은 상품만 조회한다.
- [ ] 동일 상품이 활성 상태로 이미 있으면 수량만 증가시킨다.
- [ ] soft deleted CartItem이 있으면 새 row 대신 복구할 수 있는지 검토한다.
- [ ] 수정/삭제 시 요청 회원의 장바구니 항목인지 검증한다.
- [ ] 삭제는 물리 삭제가 아니라 `deletedAt` 기반 soft delete로 처리한다.

### Repository

- [ ] 활성 CartItem 조회 시 `deletedAt is null` 조건을 포함한다.
- [ ] CartItem과 Product를 함께 쓰는 조회는 필요한 경우 fetch join을 사용한다.
- [ ] 주문 대상 CartItem 조회 시 동시성을 고려해 잠금 조회가 필요한지 확인한다.
- [ ] `cart_id + product_id` unique 제약과 soft delete 재추가 흐름이 충돌하지 않는지 확인한다.

### 테스트

- [ ] 상품 추가 성공
- [ ] 같은 상품 재추가 시 수량 증가
- [ ] 삭제된 상품 추가 실패
- [ ] 장바구니가 없으면 생성 후 추가
- [ ] 장바구니 조회 성공
- [ ] 장바구니가 없는 신규 회원은 빈 장바구니 조회
- [ ] 수량 변경 성공
- [ ] 타 회원 CartItem 접근 차단
- [ ] 상품 삭제 soft delete 성공

## Order 체크리스트

### Controller

- [ ] `POST /orders`는 일반 주문을 생성한다.
- [ ] `GET /orders`는 인증 회원의 주문 목록을 조회한다.
- [ ] `PATCH /orders/{orderId}/cancel`은 주문 취소를 처리한다.
- [ ] 생성 성공 시 필요하면 `201 CREATED`를 반환한다.
- [ ] 응답은 `ApiResponse`로 감싼다.

### Request / Response DTO

- [ ] `CreateOrderRequest.cartItemIds`는 비어 있으면 안 된다.
- [ ] `cartItemIds` 내부 ID는 양수여야 한다.
- [ ] 응답에는 주문 상태, 총액, 주문 상품 스냅샷을 담는다.
- [ ] OrderItem 응답은 주문 시점 상품명/가격을 기준으로 작성한다.

### Service

- [ ] 요청 memberId가 null이면 `UNAUTHORIZED`를 반환한다.
- [ ] 장바구니가 없으면 회원 존재 여부를 확인한 뒤 `CART_NOT_FOUND`를 반환한다.
- [ ] `cartItemIds` null/empty/중복/0 이하를 검증한다.
- [ ] 활성 CartItem만 주문 대상으로 조회한다.
- [ ] CartItem이 요청 회원의 장바구니에 속하는지 검증한다.
- [ ] 삭제된 상품은 주문 대상에서 제외한다.
- [ ] 상품별 수량을 합산해 재고 UPDATE 횟수를 줄일 수 있다.
- [ ] 재고 차감은 조건부 UPDATE 또는 비관적 락으로 동시성을 고려한다.
- [ ] 재고 부족이면 주문과 주문상품을 저장하지 않는다.
- [ ] 주문 생성 후 OrderItem 스냅샷을 저장한다.
- [ ] 주문된 CartItem은 soft delete 처리한다.
- [ ] 주문 취소는 `WAITING` 상태에서만 가능하다.
- [ ] 주문 취소 시 재고를 복구한다.

### Repository

- [ ] 주문 상태 변경 시 중복 처리를 막기 위해 필요한 경우 `PESSIMISTIC_WRITE`를 사용한다.
- [ ] 재고 차감 쿼리는 `isDeleted = false`와 `stock >= quantity` 조건을 포함한다.
- [ ] 주문 목록 조회 시 N+1이 발생하지 않도록 OrderItem을 별도 일괄 조회하거나 fetch join을 검토한다.

### 테스트

- [ ] 장바구니 상품으로 주문 생성 성공
- [ ] 재고 부족 시 주문 실패 및 주문 미저장
- [ ] 삭제된 상품 주문 실패
- [ ] 빈 cartItemIds 주문 실패
- [ ] 중복 cartItemIds 주문 실패
- [ ] 회원 주문 목록 조회 성공
- [ ] 주문이 없는 회원은 빈 목록 반환
- [ ] 결제 대기 주문 취소 성공 및 재고 복구
- [ ] 이미 취소/결제된 주문 취소 실패
- [ ] 타 회원 주문 취소 실패

## 자주 등장하는 의사결정 주제

- Cart를 회원가입 시점에 생성할지, 첫 상품 추가 시 생성할지
- CartItem 삭제를 물리 삭제로 할지 soft delete로 할지
- CartItem 식별자를 productId로 받을지 cartItemId로 받을지
- 주문 생성 대상을 전체 장바구니로 할지 선택한 CartItem 목록으로 할지
- 재고 차감을 Entity 메서드로 할지 조건부 UPDATE로 할지
- 주문 후 장바구니 항목을 삭제할지 유지할지
- 주문 상품 가격을 현재 Product에서 조회할지 OrderItem 스냅샷으로 저장할지
