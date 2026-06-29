# 주문·결제 스키마 운영 반영 안내

운영 환경은 `spring.jpa.hibernate.ddl-auto: validate`를 사용하므로 JPA가 테이블이나 컬럼을 자동 생성하지 않습니다. 주문 기능이 포함된 애플리케이션을 배포하기 전에 DBA 또는 배포 담당자가 [`sql/20260630-order-payment-schema.sql`](./sql/20260630-order-payment-schema.sql)을 운영 DB에 수동으로 적용해야 합니다.

## 반영 범위

- `orders` 테이블 생성
- `order_item` 테이블 생성
- `payments.order_type VARCHAR(20) NOT NULL` 컬럼 추가
- 기존 `payments` 데이터의 `order_type`을 `PROMOTION`으로 보정
- 주문 및 결제 조회용 인덱스와 FK 추가

`order_type`에 저장되는 값은 일반 주문의 `NORMAL`과 기존 특가 주문의 `PROMOTION`입니다.

## 배포 순서

1. 운영 DB를 백업하고 `orders`, `order_item`, `payments.order_type`의 기존 존재 여부를 확인합니다.
2. 애플리케이션 배포 전에 SQL 파일을 위에서 아래 순서로 한 번만 실행합니다.
3. 아래 확인 쿼리로 테이블, 컬럼과 기존 데이터 보정 결과를 확인합니다.
4. 애플리케이션을 배포하고 Hibernate `validate`가 통과하는지 확인합니다.

```sql
SHOW CREATE TABLE `orders`;
SHOW CREATE TABLE `order_item`;
SHOW COLUMNS FROM `payments` LIKE 'order_type';
SELECT `order_type`, COUNT(*) FROM `payments` GROUP BY `order_type`;
```

MySQL의 DDL은 실행 도중 자동 커밋될 수 있습니다. 일부 구문만 적용된 상태에서 실패했다면 SQL 전체를 다시 실행하지 말고, 위 확인 쿼리로 반영 상태를 확인한 뒤 미적용 구문부터 실행해야 합니다.
