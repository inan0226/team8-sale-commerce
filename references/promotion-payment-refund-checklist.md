# 선착순 이벤트 / 결제 / 환불 도메인 AI 체크리스트

## 1. 담당 도메인의 핵심 역할

선착순 이벤트 / 결제 / 환불 도메인은 오늘의 특가 마켓에서 사용자가 특가 상품을 선착순으로 구매하고, PortOne 결제를 승인하며, 필요 시 환불과 재고 복구까지 처리하는 핵심 거래 흐름을 담당한다.

주요 흐름은 다음과 같다.

1. 사용자가 특가 상품 선착순 구매 요청
2. 이벤트 시간, 재고, 상품 상태 검증
3. Redis Lock 기반 이벤트 재고 차감
4. 결제 전 주문 생성
5. PortOne 테스트 결제 진행
6. 서버에서 PortOne 결제 정보 조회 및 금액 검증
7. 주문 상태와 결제 상태를 PAID로 변경
8. 환불 요청 시 PortOne 결제 취소
9. 환불 상태를 REFUNDED로 변경
10. 차감했던 이벤트 재고 복구

이 도메인에서는 단순 CRUD보다 재고 정합성, 주문 상태 전이, 결제 금액 검증, 환불 시 재고 복구가 가장 중요하다.

---

## 2. 주요 Entity / DTO / API

### 주요 Entity

- PromotionProduct
  - 특가 이벤트 대상 상품 정보
  - 이벤트 시작 시간, 종료 시간, 이벤트 상태, 이벤트 재고 관리
  - 남은 이벤트 재고가 0이 되면 SOLD_OUT 처리 대상

- PromotionOrder
  - 선착순 구매로 생성되는 주문
  - 결제 전 상태로 생성되고 결제 승인 후 PAID 상태로 변경

- PromotionOrderItem
  - 선착순 주문에 포함된 상품 항목
  - 구매 수량, 단가, 총 금액 관리

- Payment
  - PortOne 결제 승인 결과 저장
  - 결제 상태, 결제 금액, PortOne Payment ID 관리

- Refund
  - 환불 요청 및 결과 저장
  - 환불 금액, 환불 상태, 환불 사유, 환불 완료 시간 관리

- StockHistory
  - 이벤트 재고 차감 / 복구 이력 저장
  - 선착순 구매, 결제 실패, 환불 등의 재고 변경 사유 추적

---

### 주요 DTO

- PromotionPurchaseRequest
  - 선착순 구매 요청 수량

- PromotionPurchaseResponse
  - 주문 ID
  - 결제 금액
  - 남은 이벤트 재고

- PaymentConfirmRequest
  - orderId
  - portOnePaymentId
  - amount

- PaymentConfirmResponse
  - 주문 ID
  - 결제 ID
  - PortOne Payment ID
  - 결제 금액
  - 주문 상태
  - 결제 상태
  - 결제 완료 시간

- RefundRequest
  - 환불 사유 타입
  - 환불 상세 사유

- RefundResponse
  - 환불 ID
  - 주문 ID
  - 환불 금액
  - 환불 상태
  - 복구 후 이벤트 재고

- RefundDetailResponse
  - 환불 상세 정보
  - 환불 금액, 환불 상태, 환불 사유, 환불 완료 시간 등

---

### 주요 API

- POST /promotions/{promotionProductId}/purchase
  - 선착순 구매 요청
  - 이벤트 시간, 재고, 상품 상태 검증 후 주문 생성
  - 이벤트 재고 차감

- POST /payments/confirm
  - PortOne 결제 승인
  - PortOne 결제 정보를 서버에서 다시 조회
  - 주문 금액과 실제 결제 금액 검증
  - 결제 성공 시 주문 상태와 결제 상태를 PAID로 변경

- POST /orders/{orderId}/refunds
  - 결제 완료된 주문 환불 요청
  - PortOne 결제 취소 API 호출
  - 환불 성공 시 환불 상태 REFUNDED 처리
  - 차감된 이벤트 재고 복구

- GET /refunds/{refundId}
  - 환불 상세 조회
  - 로그인한 사용자의 환불 정보만 조회 가능해야 함

---

## 3. 구현할 때 반드시 지켜야 하는 규칙

### 선착순 구매 규칙

- 선착순 구매는 장바구니를 거치지 않고 바로 주문을 생성한다.
- 이벤트 시작 전에는 구매할 수 없다.
- 이벤트 종료 후에는 구매할 수 없다.
- SOLD_OUT 상태의 이벤트 상품은 구매할 수 없다.
- 남은 이벤트 재고보다 많은 수량은 구매할 수 없다.
- 이벤트 재고 차감은 반드시 동시성 문제를 고려해야 한다.
- Redis Lock을 사용해 동시에 여러 요청이 들어와도 재고가 초과 차감되지 않도록 해야 한다.
- 재고 차감 성공 후 주문 생성에 실패하면 재고를 복구해야 한다.
- 남은 이벤트 재고가 0이 되면 SOLD_OUT 처리를 고려해야 한다.

### 결제 규칙

- 결제 승인은 반드시 서버에서 PortOne 결제 정보를 다시 조회한 뒤 처리한다.
- 프론트에서 전달한 결제 금액만 믿으면 안 된다.
- 주문 금액과 PortOne 실제 결제 금액이 다르면 결제 승인 실패 처리해야 한다.
- 이미 PAID 상태인 주문은 중복 결제 승인되지 않도록 해야 한다.
- 결제 승인 실패 시 필요한 경우 차감된 이벤트 재고를 복구해야 한다.
- Payment 생성과 PromotionOrder 상태 변경은 하나의 트랜잭션 흐름 안에서 일관성 있게 처리해야 한다.
- PortOne API Secret은 절대 프론트엔드나 Git에 노출하면 안 된다.
- Store ID와 Channel Key는 프론트에서 사용할 수 있지만, API Secret은 백엔드 환경변수로만 주입한다.

### 환불 규칙

- 환불은 결제 완료된 주문만 가능하다.
- 결제 상태가 PAID가 아닌 주문은 환불할 수 없다.
- 이미 환불된 주문은 중복 환불되지 않도록 해야 한다.
- 환불 요청 시 PortOne 결제 취소 API 호출이 먼저 성공해야 한다.
- PortOne 결제 취소 성공 후 Refund 상태를 REFUNDED로 변경한다.
- 환불 성공 시 선착순 구매 때 차감한 이벤트 재고를 복구해야 한다.
- 환불로 재고가 복구되면 SOLD_OUT 상태였던 이벤트 상품의 상태 전환도 고려해야 한다.
- 환불 상세 조회는 본인의 환불 내역만 조회 가능해야 한다.

---

## 4. 자주 발생했던 에러나 트러블슈팅 포인트

### PortOne 결제 승인 실패

증상:
- PortOne 결제는 성공했지만 서버 결제 승인 API에서 실패한다.

주요 원인:
- PORTONE_API_SECRET 환경변수가 서버 실행 시 주입되지 않음
- Store ID, Channel Key, API Secret이 서로 다른 PortOne 프로젝트의 값임
- PortOne Payment ID가 서버에 잘못 전달됨
- 주문 금액과 PortOne 실제 결제 금액이 다름

확인 방법:
- 서버 실행 명령어에 PORTONE_API_SECRET, PORTONE_STORE_ID가 포함되어 있는지 확인
- 프론트 로그에서 orderId, portOnePaymentId, amount 확인
- 서버 로그에서 PortOne 결제 조회 실패 여부 확인

---

### demo.html에서 버튼 클릭이 안 되는 문제

증상:
- 버튼을 눌러도 아무 반응 없음
- 브라우저 콘솔에 refundOrder is not defined 발생

원인:
- HTML onclick에서 호출하는 JS 함수가 실제로 정의되어 있지 않음
- script 내부 함수가 window 객체에 등록되지 않음

해결:
- refundOrder 함수를 추가
- 필요한 경우 window.refundOrder = refundOrder 형태로 전역 등록

---

### @DataJpaTest에서 JPAQueryFactory Bean을 찾지 못하는 문제

증상:
- Repository 테스트 실행 시 JPAQueryFactory Bean 등록 실패

원인:
- @DataJpaTest는 슬라이스 테스트라서 main의 QueryDslConfig가 자동으로 로드되지 않음

해결:
- 필요한 테스트 클래스에 @Import(QueryDslConfig.class) 추가
- JpaAuditingConfig가 필요한 경우 함께 import

예시:

    @DataJpaTest
    @Import({JpaAuditingConfig.class, QueryDslConfig.class})
    class ExampleRepositoryTest {
    }

---

### application.yml Secret 노출 위험

주의:
- DB 비밀번호, PortOne API Secret을 application.yml에 직접 작성하면 안 된다.

권장:

    portone:
      api:
        secret: ${PORTONE_API_SECRET:}
      store-id: ${PORTONE_STORE_ID:}

---

## 5. 테스트할 때 꼭 확인해야 하는 케이스

### 선착순 구매 테스트

- 이벤트 시작 전 구매 요청 시 실패
- 이벤트 종료 후 구매 요청 시 실패
- 이벤트 재고가 충분하면 구매 성공
- 이벤트 재고가 부족하면 구매 실패
- SOLD_OUT 상태 상품 구매 실패
- 구매 성공 시 이벤트 재고 차감
- 구매 성공 시 주문 생성
- 동시 요청 시 재고가 음수가 되지 않음
- 동시 요청 시 재고보다 많은 주문이 생성되지 않음

### 결제 테스트

- 정상 결제 승인 시 주문 상태 PAID 변경
- 정상 결제 승인 시 Payment 상태 PAID 저장
- 주문 금액과 결제 금액이 다르면 실패
- 존재하지 않는 주문 결제 승인 실패
- 다른 사용자의 주문 결제 승인 실패
- 이미 결제 완료된 주문 중복 승인 실패
- PortOne 결제 조회 실패 시 결제 승인 실패
- 결제 승인 실패 시 필요한 재고 복구 처리 확인

### 환불 테스트

- 결제 완료된 주문 환불 성공
- 환불 성공 시 Refund 상태 REFUNDED 변경
- 환불 성공 시 이벤트 재고 복구
- 결제 전 주문 환불 실패
- 이미 환불된 주문 중복 환불 실패
- 다른 사용자의 주문 환불 실패
- PortOne 결제 취소 실패 시 Refund 저장 또는 재고 복구가 잘못 발생하지 않는지 확인
- 환불 상세 조회 시 본인 환불만 조회 가능

### 통합 시연 테스트

- 회원가입
- 로그인
- 선착순 구매
- PortOne 테스트 결제
- 서버 결제 승인
- 환불 요청
- 환불 상세 조회
- 구매 시 재고 10에서 9로 차감 확인
- 환불 시 재고 9에서 10으로 복구 확인

---

## 6. AI가 코드 작성할 때 조심해야 할 금지사항

- 선착순 구매를 일반 장바구니 주문 흐름으로 우회해서 구현하면 안 된다.
- 이벤트 재고 차감 없이 주문을 먼저 생성하면 안 된다.
- Redis Lock 없이 단순 조회 후 재고 차감 방식으로 동시성 처리를 끝내면 안 된다.
- 프론트에서 전달한 amount만 믿고 결제 승인하면 안 된다.
- PortOne 결제 조회 없이 결제 성공 처리하면 안 된다.
- PortOne API Secret을 demo.html, application.yml 실제 값, README, PR 본문에 노출하면 안 된다.
- 결제 실패나 환불 성공 시 재고 복구를 누락하면 안 된다.
- 이미 PAID 또는 REFUNDED 상태인 주문을 중복 처리하면 안 된다.
- 다른 사용자의 주문, 결제, 환불 정보를 조회하거나 처리하면 안 된다.
- 테스트에서 QueryDslConfig가 필요한데 @Import를 누락하면 안 된다.
- 테스트 통과를 위해 실제 운영 로직 검증을 제거하면 안 된다.
- 재고 정합성과 상태 전이 검증을 단순히 프론트 화면 기준으로만 판단하면 안 된다.
- 예외를 모두 RuntimeException으로 처리하지 말고 프로젝트의 공통 예외 처리 방식을 따라야 한다.
- DTO는 가능하면 record를 사용하고, 응답 변환은 Response.from(entity) 형태의 팀 컨벤션을 우선한다.
- 도메인 객체 생성은 생성자 직접 호출보다 정적 팩토리 메서드를 우선한다.
