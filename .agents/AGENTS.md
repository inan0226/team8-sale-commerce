# Database Schema (ER Diagram)

Below is the database schema for the application, representing the entities and their relationships.

```mermaid
erDiagram

    MEMBER {
        BIGINT id PK
        VARCHAR email UK
        VARCHAR password
        VARCHAR nickname UK
        VARCHAR role
        DATETIME created_at
        DATETIME modified_at
    }

    CATEGORY {
        BIGINT id PK
        VARCHAR name UK
        DATETIME created_at
        DATETIME modified_at
    }

    PRODUCT {
        BIGINT id PK
        BIGINT category_id FK
        VARCHAR name
        VARCHAR brand
        TEXT description
        BIGINT price
        INT stock
        VARCHAR image_url
        BOOLEAN is_deleted
        BIGINT view_count
        DATETIME created_at
        DATETIME modified_at
    }

    PROMOTION_PRODUCT {
        BIGINT id PK
        BIGINT product_id FK
        VARCHAR title
        BIGINT promotion_price
        INT discount_rate
        INT event_stock
        VARCHAR status
        DATETIME start_time
        DATETIME end_time
        DATETIME created_at
        DATETIME modified_at
    }

    CART {
        BIGINT id PK
        BIGINT member_id FK
        DATETIME created_at
        DATETIME modified_at
    }

    CART_ITEM {
        BIGINT id PK
        BIGINT cart_id FK
        BIGINT product_id FK
        INT quantity
        DATETIME created_at
        DATETIME modified_at
    }

    ORDERS {
        BIGINT id PK
        BIGINT member_id FK
        BIGINT total_price
        VARCHAR status
        DATETIME ordered_at
        DATETIME created_at
        DATETIME modified_at
    }

    ORDER_ITEM {
        BIGINT id PK
        BIGINT order_id FK
        BIGINT product_id FK
        VARCHAR product_name
        BIGINT product_price
        INT quantity
        DATETIME created_at
        DATETIME modified_at
    }

    PROMOTION_ORDER_ITEM {
        BIGINT id PK
        BIGINT order_id FK
        BIGINT promotion_product_id FK
        VARCHAR product_name
        BIGINT original_price
        BIGINT promotion_price
        INT discount_rate
        INT quantity
        DATETIME created_at
        DATETIME modified_at
    }

    PAYMENT {
        BIGINT id PK
        BIGINT order_id FK
        VARCHAR portone_payment_id UK
        BIGINT amount
        VARCHAR method
        VARCHAR status
        DATETIME paid_at
        DATETIME failed_at
        VARCHAR failure_reason
        DATETIME created_at
        DATETIME modified_at
    }

    REFUND {
        BIGINT id PK
        BIGINT order_id FK
        BIGINT payment_id FK
        BIGINT member_id FK
        VARCHAR reason_type
        TEXT reason_detail
        BIGINT refund_amount
        VARCHAR status
        DATETIME requested_at
        DATETIME completed_at
        DATETIME created_at
        DATETIME modified_at
    }

    STOCK_HISTORY {
        BIGINT id PK
        BIGINT product_id FK
        BIGINT promotion_product_id FK
        BIGINT order_id FK
        BIGINT payment_id FK
        BIGINT refund_id FK
        VARCHAR type
        INT quantity
        INT stock_before
        INT stock_after
        VARCHAR reason
        DATETIME created_at
    }

    CHAT_ROOM {
        BIGINT id PK
        BIGINT member_id FK
        VARCHAR status
        DATETIME created_at
        DATETIME modified_at
    }

    CHAT_MESSAGE {
        BIGINT id PK
        BIGINT chat_room_id FK
        BIGINT sender_id FK
        VARCHAR sender_type
        TEXT message
        DATETIME created_at
    }

    MEMBER ||--|| CART : owns
    MEMBER ||--o{ ORDERS : places
    MEMBER ||--o{ REFUND : requests
    MEMBER ||--o{ CHAT_ROOM : opens
    MEMBER ||--o{ CHAT_MESSAGE : sends

    CATEGORY ||--o{ PRODUCT : categorizes

    PRODUCT ||--o{ CART_ITEM : added
    PRODUCT ||--o{ ORDER_ITEM : ordered
    PRODUCT ||--o{ PROMOTION_PRODUCT : promoted
    PRODUCT ||--o{ STOCK_HISTORY : stock_logged

    PROMOTION_PRODUCT ||--o{ PROMOTION_ORDER_ITEM : ordered
    PROMOTION_PRODUCT ||--o{ STOCK_HISTORY : event_stock_logged

    CART ||--o{ CART_ITEM : contains

    ORDERS ||--o{ ORDER_ITEM : contains
    ORDERS ||--o{ PROMOTION_ORDER_ITEM : contains
    ORDERS ||--o| PAYMENT : paid_by
    ORDERS ||--o{ REFUND : refunded
    ORDERS ||--o{ STOCK_HISTORY : changes_stock

    PAYMENT ||--o{ REFUND : refund_source
    PAYMENT ||--o{ STOCK_HISTORY : payment_stock_logged

    REFUND ||--o{ STOCK_HISTORY : restores_stock

    CHAT_ROOM ||--o{ CHAT_MESSAGE : contains
```

# User Flow (Flowchart)

Below is the flowchart representing the core user flows of the application (Product Search, Product Detail, Category list).

```mermaid
flowchart TD
    A["시작"] --> B{"요청 종류"}

    B -->|상품 검색| C["검색어 입력"]
    C --> D["최소금액 / 최대금액 입력 선택"]
    D --> E["GET /search/products 호출"]
    E --> F["검색 조건 구성"]
    F --> G["상품명 / 브랜드명 / 카테고리 조건"]
    G --> H["가격 필터 조건"]
    H --> I["DB 상품 조회"]
    I --> J["검색 결과 반환"]
    J --> Z1["종료"]

    B -->|상품 상세 조회| K["상품 상세 조회 요청"]
    K --> L["상품 ID 검증"]
    L --> M["상품 조회"]
    M --> N{"상품 존재?"}
    N -->|아니오| O["상품 없음 응답"]
    N -->|예| P["상품 정보 반환"]
    P --> Z2["종료"]

    B -->|카테고리 조회| Q["카테고리 조회 요청"]
    Q --> R["카테고리 목록 조회"]
    R --> S["카테고리 목록 반환"]
    S --> Z3["종료"]
```
