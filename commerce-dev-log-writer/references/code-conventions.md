# Code Conventions

8 team Sale Commerce 프로젝트에서 코드 예시, 리뷰 체크리스트, 문서화 기준을 작성할 때 따르는 규칙이다.

## 패키지와 도메인 분리

- 도메인 기능은 도메인별 패키지에 둔다.
- 공통 기능은 `global`에 둔다.
- 실제 저장소에서는 `com.example.team8salecommerce.domain.*`, `com.example.team8salecommerce.global.*` 구조를 사용한다.

## 클래스 네이밍

| 종류 | 규칙 | 예시 |
| --- | --- | --- |
| Controller | `도메인Controller` | `MemberController`, `CartController` |
| Service | `도메인Service` | `AuthService`, `OrderService` |
| Repository | `도메인Repository` | `MemberRepository`, `CartRepository` |
| Entity | 단수형 | `Member`, `Product`, `Order` |
| Request DTO | `기능Request` | `LoginRequest`, `CreateOrderRequest` |
| Response DTO | `기능Response` | `LoginResponse`, `OrderResponse` |
| Exception | `도메인Exception` 또는 공통 `CustomException` | `ProductException`, `CustomException` |
| Enum | 명확한 역할명 | `OrderStatus`, `PaymentStatus` |

## 메서드 네이밍

- 조회: `getMember()`, `getProduct()`, `getOrders()`
- 생성: `createOrder()`, `createChatRoom()`
- 수정: `updateCartItemQuantity()`, `updateMemberProfile()`
- 삭제: `deleteCartItem()`
- 검증: `validatePassword()`, `validateStock()`, `validatePromotionOpen()`
- 인증: `login()`, `signup()`, `logout()`
- Boolean 반환: `isSoldOut()`, `hasEnoughStock()`, `canCancel()`

## DTO 규칙

- DTO는 Java `record` 사용을 우선한다.
- Controller에서는 Entity를 직접 받거나 반환하지 않는다.
- Request/Response DTO는 역할별로 분리한다.

좋은 예:

```java
public record CreateOrderRequest(
    List<Long> cartItemIds
) {
}
```

피해야 할 예:

```java
public ResponseEntity<Order> getOrder(...) {
    ...
}
```

## API 응답 규칙

- Controller 응답은 `ResponseEntity<ApiResponse<T>>` 형태를 사용한다.
- 성공 응답은 `ApiResponse.success(...)`를 사용한다.

예:

```java
return ResponseEntity.ok(ApiResponse.success(response));
```

또는 메시지가 필요한 경우:

```java
return ResponseEntity.status(HttpStatus.CREATED)
    .body(ApiResponse.success("주문 생성 성공", response));
```

## URL 컨벤션

RESTful하게 작성한다.

```text
GET    /cart
POST   /cart/items
PATCH  /cart/items/{cartItemId}
DELETE /cart/items/{cartItemId}
POST   /orders
GET    /orders
PATCH  /orders/{orderId}/cancel
```

실제 프로젝트 API 경로가 컨벤션 예시와 다르면 실제 Controller 경로를 우선한다.

## 변수명 / DB 컬럼

- Java 변수명: camelCase
  - `memberId`, `productId`, `promotionProductId`, `totalPaymentPrice`
- DB 컬럼명: snake_case
  - `member_id`, `product_id`, `promotion_product_id`, `total_payment_price`

## Entity 규칙

- Entity에는 비즈니스 메서드를 둔다.
- Setter는 최소화한다.
- Entity에는 `@Data`를 사용하지 않는다.
- 객체 생성 시 비즈니스 규칙이 포함되면 정적 팩토리 메서드 `create()`를 사용한다.

권장:

```java
product.decreaseStock(quantity);
```

지양:

```java
product.setStock(product.getStock() - quantity);
```

## 예외 처리 규칙

- 예외는 `ErrorCode` enum으로 관리한다.
- 공통 예외는 `CustomException(ErrorCode.X)`를 사용한다.
- 도메인 전용 예외가 이미 존재하면 해당 도메인 예외를 사용한다.
- 원인을 알 수 없는 `IllegalArgumentException` 직접 throw는 피한다.

예:

```java
throw new CustomException(ErrorCode.PRODUCT_NOT_FOUND);
```

## 인증 규칙

- 인증된 사용자의 식별자는 request body나 query parameter로 받지 않는다.
- Controller에서 `@AuthenticationPrincipal AuthMember authMember`를 받고 `AuthMemberResolver.requireMemberId(authMember)`로 꺼낸다.
- 인증 정보가 없으면 `UNAUTHORIZED`, 권한/소유권이 없으면 `FORBIDDEN`을 사용한다.

## 테스트 규칙

- 테스트 이름은 한글 허용을 권장한다.
- 의도가 바로 보이도록 작성한다.

예:

```java
@Test
void 로그인_성공() {
}

@Test
void 재고가_부족하면_주문에_실패한다() {
}
```

현재 저장소에는 `@DisplayName`과 영어 메서드명을 함께 쓰는 테스트도 있다. 기존 파일 스타일을 우선 따른다.

Mockito `save()` mocking 시 null 반환으로 NPE가 나지 않도록 다음 패턴을 사용한다.

```java
when(repository.save(any(Entity.class)))
    .thenAnswer(invocation -> invocation.getArgument(0));
```

## Git 브랜치 / 커밋 메시지

브랜치 예:

```text
main
develop
feature/member-auth
feature/product-search
feature/cart-order
feature/promotion-lock
feature/chat
fix/login-token-error
```

커밋 메시지 예:

```text
feat: 회원가입 기능 구현
fix: 장바구니 수량 변경 오류 수정
refactor: 주문 생성 로직 분리
test: Redis Lock 동시성 테스트 추가
docs: ERD 문서 추가
```

## 팀원별 담당 영역

| 담당 | 패키지 |
| --- | --- |
| 김인안 | `member`, `auth`, `security`, `chat` |
| 이지영 | `product`, `category`, `search` |
| 정지수 | `cart`, `order` |
| 임선구 | `promotion`, `payment`, `refund`, `stock` |

공통 파일인 `global`, `security`, `application.yml`은 수정 전 팀원에게 공유하는 방향으로 안내한다.

## PR 전 체크리스트

```markdown
- [ ] 빌드 성공
- [ ] 테스트 통과
- [ ] Entity 직접 반환 없음
- [ ] Controller에 비즈니스 로직 없음
- [ ] 예외 메시지 통일
- [ ] API URL 규칙 준수
- [ ] 본인 담당 패키지 외 수정 시 팀원에게 공유
```
