# Sale Commerce

한정 수량 특가 판매와 일반 상품 주문을 함께 지원하는 커머스 백엔드입니다.  
상품 탐색부터 장바구니, 주문, PortOne 결제·환불, 실시간 상담 채팅까지 하나의 Spring Boot 애플리케이션으로 구성했습니다.

## 프로젝트 핵심

- **일반 상품 주문**: 장바구니 기반 주문 생성, 재고 차감, 주문 조회 및 취소
- **선착순 특가 구매**: 상품 단위 Redis 분산 락으로 동시 구매 요청과 재고 정합성 제어
- **안전한 결제 처리**: PortOne 결제 정보·금액 검증, 중복 결제 방지, 주문 유형별 결제 처리
- **환불 보상 흐름**: 외부 환불과 내부 상태 변경을 단계별 트랜잭션으로 분리하고 특가 재고 복구
- **검색과 랭킹**: 조건별 상품 검색 결과 캐싱 및 Redis Sorted Set 기반 인기 검색어 집계
- **실시간 채팅**: STOMP/WebSocket 기반 메시지 송수신, JWT 연결 인증 및 대화 이력 저장
- **인증과 권한**: JWT 기반 Stateless 인증, Redis Refresh Token·Access Token 블랙리스트, USER/ADMIN 권한 분리

## 기술 스택

| 구분 | 기술 |
| --- | --- |
| Language | Java 21 |
| Framework | Spring Boot 4.1, Spring MVC, Spring Security |
| Data | Spring Data JPA, MySQL, H2(Test) |
| Cache / Lock | Redis, Spring Cache, Redisson |
| Realtime | WebSocket, STOMP |
| Authentication | JWT (JJWT), BCrypt |
| Payment | PortOne V2 REST API |
| Test | JUnit 5, Spring Boot Test, Testcontainers |
| Build | Gradle |

## 시스템 아키텍처

> 시스템 아키텍처 이미지를 추가할 예정입니다.

<!-- 시스템 아키텍처 이미지 삽입 -->

Redis는 검색 결과 캐시, 인기 검색어, Refresh Token과 로그아웃 블랙리스트, 특가 상품 분산 락에 사용됩니다. MySQL 비관적 락과 Redis 분산 락을 용도에 맞게 적용해 주문·결제·환불 과정의 중복 처리와 재고 경합을 방지합니다.

## ERD

<img width="2048" height="3523" alt="diagram (1)" src="https://github.com/user-attachments/assets/ea783f5c-e4cb-4f2f-b8e5-d8fb05dca272" />


상세 엔티티 관계와 테이블 설명은 [ERD 문서](docs/erd.md)에서 확인할 수 있습니다.

## 주요 처리 흐름

### 선착순 구매

1. 특가 상품 ID를 기준으로 Redis 락을 획득합니다.
2. 판매 시간, 상품 상태, 잔여 수량을 다시 검증합니다.
3. 재고를 차감하고 `WAITING` 상태의 특가 주문과 재고 이력을 생성합니다.
4. 결제에 필요한 주문 정보를 반환하고 락을 안전하게 해제합니다.

### 결제와 환불

1. 결제 승인 요청 시 주문을 잠그고 소유자와 주문 상태를 검증합니다.
2. PortOne에서 실제 결제 상태와 금액을 조회합니다.
3. 결제 ID의 유일성을 보장한 뒤 결제·주문 상태를 변경합니다.
4. 결제 실패 또는 환불 완료 시 주문 유형에 맞춰 상품 재고를 복구합니다.
5. 외부 환불 성공 정보와 내부 완료 처리를 분리해 부분 실패를 추적할 수 있도록 합니다.

더 자세한 시퀀스와 상태 변화는 [주요 기능 흐름](docs/flows.md), [ERD](docs/erd.md)에서 확인할 수 있습니다.

## 도메인별 설계 포인트

| 도메인 | 설계 포인트 |
| --- | --- |
| Auth | Access/Refresh Token을 분리하고 Refresh Token은 Redis에 저장합니다. 로그아웃한 Access Token은 남은 만료 시간 동안 블랙리스트에 등록해 Stateless 인증에서도 즉시 무효화합니다. |
| Member | 이메일과 닉네임 중복을 방지하고 비밀번호는 BCrypt로 단방향 암호화합니다. `USER`, `ADMIN` 역할을 JWT Claim에 포함해 API와 채팅방 접근 범위를 구분합니다. |
| Product | 목록·상세 조회를 분리하고 가격, 정렬, 페이징 조건을 DTO로 반환합니다. 삭제된 상품은 조회와 주문 대상에서 제외해 기존 데이터는 유지하면서 판매만 중단합니다. |
| Category | 카테고리를 기준으로 판매 가능한 상품을 조회하고, 카테고리 존재 여부와 상품 조회 책임을 서비스 계층에서 분리합니다. |
| Search | JPA Specification으로 선택적인 검색 조건을 조합하고 검색 조건 전체를 캐시 키로 사용합니다. 검색어 횟수는 Redis Sorted Set, 마지막 검색 시각은 Hash로 관리합니다. |
| Cart | 회원별 장바구니 소유권을 검증하고 동일 상품을 다시 담으면 새 항목 대신 수량을 증가시킵니다. 항목 삭제는 soft delete로 처리해 재추가와 이력 보존을 함께 고려합니다. |
| Order | 주문 생성 시 상품 재고를 비관적 락으로 조회해 동시에 발생한 일반 주문의 재고 경합을 제어합니다. 주문 상태를 `WAITING`, `PAID`, `PAYMENT_FAILED`, `CANCELLED`, `REFUND_REQUEST`, `REFUNDED`로 명시합니다. |
| Promotion | 특가 상품 ID 단위 Redisson 분산 락을 사용하고 락 획득 후 판매 시간·상태·재고를 다시 검증합니다. 일반 주문과 분리된 특가 주문 모델로 선착순 구매 흐름을 독립시킵니다. |
| Stock | 수량 변경과 함께 증가·감소 유형, 변경 사유, 주문 참조를 재고 이력으로 남겨 결제 실패와 환불에 따른 복구 과정을 추적할 수 있게 합니다. |
| Payment | 클라이언트의 결제 결과를 그대로 신뢰하지 않고 PortOne에서 상태와 금액을 재조회합니다. 외부 호출 뒤 짧은 트랜잭션에서 주문 row lock과 결제 ID 유니크 제약으로 중복 승인을 방지합니다. |
| Refund | 환불 요청, PortOne 성공 기록, 내부 완료 처리를 각각 독립된 트랜잭션으로 분리합니다. 외부 환불 성공 후 내부 처리가 실패해도 중간 상태가 DB에 남도록 설계했습니다. |
| Chat | REST로 채팅방·이력을 관리하고 STOMP로 메시지를 전달합니다. `CONNECT` 프레임의 JWT를 인터셉터에서 인증하고 회원은 자신의 방만, 관리자는 전체 방을 조회할 수 있습니다. |
| Global | 성공 응답과 예외 형식을 공통화하고 JWT 필터, Redis Cache, Redisson Lock, WebSocket 인증처럼 여러 도메인이 공유하는 관심사를 별도 패키지로 분리했습니다. |

## 트러블 슈팅

### 1. 선착순 요청에서 재고보다 많은 주문이 생성되는 문제

**문제**  
여러 요청이 동시에 같은 특가 상품의 재고를 읽으면 각각 재고가 남아 있다고 판단해 초과 주문이나 음수 재고가 발생할 수 있었습니다. 애플리케이션 내부의 동기화만으로는 다중 인스턴스 환경을 보호할 수도 없었습니다.

**해결**

- `promotionProductId`를 포함한 `lock:promotion-product:{id}` 키로 Redisson 분산 락을 획득했습니다.
- 락을 얻은 뒤 상품을 다시 조회하고 판매 시간, 상태, 잔여 재고를 재검증했습니다.
- 구매뿐 아니라 결제 실패와 환불의 재고 복구도 동일한 lock key 규칙을 사용했습니다.
- DB 조회에는 비관적 쓰기 락을 함께 적용해 분산 락과 데이터베이스 갱신 사이의 정합성을 보강했습니다.

**결과**  
구매와 재고 복구가 상품 단위로 직렬화되어 재고보다 많은 주문이 생성되는 경로를 차단했고, 동시성 테스트로 성공 주문 수와 잔여 재고의 일관성을 검증할 수 있게 되었습니다.

### 2. 결제 승인 중 DB 락이 길어지고 중복 결제가 저장되는 문제

**문제**  
PortOne 조회처럼 지연 시간이 일정하지 않은 외부 API 호출을 DB 트랜잭션 안에서 수행하면 주문 row lock 보유 시간이 길어집니다. 같은 주문의 승인 요청이 동시에 들어오면 동일 결제를 중복 저장할 가능성도 있었습니다.

**해결**

- 트랜잭션 밖에서 주문을 1차 검증하고 PortOne 결제 상태와 실제 금액을 조회했습니다.
- 외부 호출이 끝난 뒤 짧은 트랜잭션을 시작해 주문 row lock을 획득하고 상태와 금액을 다시 검증했습니다.
- `portOnePaymentId`에 DB 유니크 제약을 두고 동시 요청의 최종 방어선으로 사용했습니다.
- 일반 주문과 특가 주문의 승인 로직을 분리하면서 공통 진입점에서 주문 유형에 맞는 서비스로 라우팅했습니다.

**결과**  
외부 API 응답을 기다리는 동안 DB 락을 점유하지 않아 경합 범위를 줄였고, 애플리케이션 검증과 DB 제약을 함께 사용해 중복 승인에 대비했습니다.

### 3. PortOne 환불은 성공했지만 내부 상태 변경이 실패하는 문제

**문제**  
외부 PortOne 환불과 내부 주문·결제·재고 갱신은 하나의 원자적 트랜잭션으로 묶을 수 없습니다. 외부 환불 성공 직후 내부 트랜잭션이 실패하면 실제 결제는 취소됐지만 서버에는 환불 여부가 남지 않는 불일치가 생길 수 있었습니다.

**해결**

- 환불 요청을 먼저 `REFUND_REQUEST` 상태로 저장한 후 PortOne API를 호출했습니다.
- 외부 환불 성공 결과를 별도 트랜잭션에서 `PORTONE_REFUND_SUCCEEDED`로 먼저 기록했습니다.
- 이후 내부 주문 상태 변경과 특가 재고 복구를 수행하고 최종적으로 `REFUNDED` 상태로 전환했습니다.
- 외부 호출 실패는 `REFUND_FAILED`로 기록해 성공·실패 원인을 상태로 구분했습니다.

**결과**  
외부 환불 성공과 내부 완료 사이의 중간 상태가 사라지지 않아 장애 지점을 추적할 수 있고, 미완료 건을 식별해 후속 처리할 근거를 확보했습니다.

## API 개요

| 도메인 | Method | Endpoint | 설명 | 권한 |
| --- | --- | --- | --- | --- |
| 인증 | POST | `/auth/signup` | 회원가입 | Public |
| 인증 | POST | `/auth/login` | 로그인 및 토큰 발급 | Public |
| 인증 | POST | `/auth/logout` | 로그아웃 및 토큰 무효화 | USER |
| 회원 | GET | `/members/me` | 내 정보 조회 | USER |
| 상품 | GET | `/products` | 상품 목록 조회 | Public |
| 상품 | GET | `/products/{productId}` | 상품 상세 조회 | Public |
| 카테고리 | GET | `/categories/{categoryId}/products` | 카테고리별 상품 조회 | Public |
| 검색 | GET | `/products/search` | 조건별 상품 검색 | Public |
| 검색 | GET | `/search-keywords/top` | 인기 검색어 TOP 10 | Public |
| 검색 | GET | `/search-keywords` | 전체 검색어 통계 | ADMIN |
| 장바구니 | POST | `/cart/items` | 상품 추가 | USER |
| 장바구니 | GET | `/cart` | 장바구니 조회 | USER |
| 장바구니 | PATCH | `/cart/items/{cartItemId}` | 수량 변경 | USER |
| 장바구니 | DELETE | `/cart/items/{cartItemId}` | 상품 삭제 | USER |
| 주문 | POST | `/orders` | 일반 주문 생성 | USER |
| 주문 | GET | `/orders` | 내 주문 목록 조회 | USER |
| 주문 | PATCH | `/orders/{orderId}/cancel` | 주문 취소 | USER |
| 특가 | POST | `/promotions/{promotionProductId}/purchase` | 선착순 구매 | USER |
| 결제 | POST | `/payments/confirm` | 결제 승인 | USER |
| 결제 | POST | `/payments/fail` | 결제 실패 처리 | USER |
| 환불 | POST | `/orders/{orderId}/refunds` | 환불 요청 | USER |
| 환불 | GET | `/refunds/{refundId}` | 환불 상태 조회 | USER |
| 채팅 | GET | `/chat/rooms` | 채팅방 목록 조회 | USER |
| 채팅 | POST | `/chat/rooms` | 채팅방 생성 | USER |
| 채팅 | GET | `/chat/rooms/{chatRoomId}/messages` | 메시지 이력 조회 | USER |
| 채팅 | PATCH | `/chat/rooms/{chatRoomId}/status` | 채팅방 상태 변경 | USER |

인증이 필요한 REST API는 다음 헤더를 사용합니다.

```http
Authorization: Bearer {accessToken}
```

모든 REST 응답은 공통 형식을 따릅니다.

```json
{
  "success": true,
  "message": "요청이 성공했습니다.",
  "data": {}
}
```

### WebSocket

- Handshake endpoint: `/ws/chat`
- Publish destination: `/pub/chat/message`
- Subscribe destination: `/sub/chat/rooms/{chatRoomId}`
- STOMP `CONNECT` 프레임의 `Authorization` 헤더에 Bearer Token이 필요합니다.

## 로컬 실행

### 1. 요구 사항

- JDK 21
- MySQL 8.x
- Redis
- Docker (통합 테스트 실행 시)

### 2. 데이터베이스 생성

```sql
CREATE DATABASE team8_sale_commerce
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
```

운영 설정은 Hibernate `validate` 모드를 사용하므로 테이블 스키마가 먼저 준비되어 있어야 합니다. 최초 로컬 실행에서만 `SPRING_JPA_HIBERNATE_DDL_AUTO=update`로 스키마를 만든 뒤, 이후에는 해당 환경 변수를 제거해 `validate`를 사용하는 것을 권장합니다. 기존 주문·결제 스키마를 변경하는 경우에는 [마이그레이션 안내](docs/order-payment-schema-change.md)와 [SQL](docs/sql/20260630-order-payment-schema.sql)을 참고하세요.

### 3. 환경 변수 설정

PowerShell 예시입니다.

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/team8_sale_commerce?serverTimezone=Asia/Seoul&characterEncoding=UTF-8"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="your-password"

$env:SPRING_DATA_REDIS_HOST="localhost"
$env:SPRING_DATA_REDIS_PORT="6379"
$env:JWT_SECRET="replace-with-a-secure-secret-key-at-least-32-bytes"

# 실제 결제·환불 연동 시 설정
$env:PORTONE_API_SECRET="your-portone-api-secret"
$env:PORTONE_STORE_ID="your-portone-store-id"

# 최초 로컬 스키마 생성 시에만 사용
$env:SPRING_JPA_HIBERNATE_DDL_AUTO="update"
```

| 환경 변수 | 필수 | 기본값 / 설명 |
| --- | --- | --- |
| `DB_URL` | Yes | MySQL JDBC URL |
| `DB_USERNAME` | Yes | MySQL 사용자명 |
| `DB_PASSWORD` | Yes | MySQL 비밀번호 |
| `SPRING_DATA_REDIS_HOST` | No | `localhost` |
| `SPRING_DATA_REDIS_PORT` | No | `6379` |
| `JWT_SECRET` | Production | 로컬 기본값이 있으나 배포 환경에서는 반드시 변경 |
| `JWT_ACCESS_TOKEN_EXPIRATION` | No | `1800000` ms (30분) |
| `JWT_REFRESH_TOKEN_EXPIRATION` | No | `1209600000` ms (14일) |
| `PORTONE_API_SECRET` | 결제 연동 시 | PortOne V2 API Secret |
| `PORTONE_STORE_ID` | 환불 연동 시 | PortOne Store ID |
| `WEBSOCKET_ALLOWED_ORIGIN_PATTERNS` | No | `*` |

### 4. 애플리케이션 실행

Windows:

```powershell
.\gradlew.bat bootRun
```

macOS / Linux:

```bash
./gradlew bootRun
```

## 테스트

일반 테스트는 H2를 사용하며 외부 인프라가 필요한 통합 테스트를 제외합니다.

```powershell
.\gradlew.bat test
```

Docker/Testcontainers 기반 통합 테스트는 별도 태스크로 실행합니다.

```powershell
.\gradlew.bat integrationTest
```

전체 검증:

```powershell
.\gradlew.bat clean test integrationTest
```

## 프로젝트 구조

```text
src/main/java/com/example/team8salecommerce
├── domain
│   ├── auth          # 회원가입, 로그인, 로그아웃, 토큰 관리
│   ├── member        # 회원 정보와 권한
│   ├── product       # 상품 목록과 상세 조회
│   ├── category      # 카테고리별 상품 조회
│   ├── search        # 상품 검색과 인기 검색어
│   ├── cart          # 장바구니 상품 관리
│   ├── order         # 일반 주문과 주문 상품
│   ├── promotion     # 선착순 특가 상품과 주문
│   ├── stock         # 재고 변경 이력
│   ├── payment       # PortOne 결제 승인·실패 처리
│   ├── refund        # 환불 요청·완료·실패 처리
│   └── chat          # 채팅방과 실시간 메시지
└── global
    ├── config        # JPA, Redis, Cache 설정
    ├── exception     # 공통 예외와 에러 코드
    ├── response      # 공통 API 응답
    ├── security      # JWT 인증·인가
    ├── util          # Redis 분산 락
    └── websocket     # STOMP 및 WebSocket 인증
```

## 관련 문서

- [주요 API 처리 흐름](docs/flows.md)
- [ERD](docs/erd.md)
- [주문·결제 스키마 변경 안내](docs/order-payment-schema-change.md)
- [주문·결제 스키마 변경 SQL](docs/sql/20260630-order-payment-schema.sql)
