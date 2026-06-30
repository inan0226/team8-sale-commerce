-- 주문 도메인 배포 전에 운영 DB에 수동으로 적용하는 MySQL DDL입니다.
-- application.yml의 ddl-auto가 validate이므로 애플리케이션 배포보다 먼저 실행해야 합니다.

-- 일반 주문의 주문 헤더를 저장합니다.
CREATE TABLE `orders` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `member_id` BIGINT NOT NULL,
    `total_price` BIGINT NOT NULL,
    `status` VARCHAR(30) NOT NULL,
    `ordered_at` DATETIME(6) NOT NULL,
    `created_at` DATETIME(6) NOT NULL,
    `modified_at` DATETIME(6) NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_orders_member_ordered_at` (`member_id`, `ordered_at`),
    CONSTRAINT `fk_orders_member`
        FOREIGN KEY (`member_id`) REFERENCES `member` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 주문 시점의 상품명, 가격과 수량을 스냅샷으로 저장합니다.
CREATE TABLE `order_item` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `order_id` BIGINT NOT NULL,
    `product_id` BIGINT NOT NULL,
    `product_name` VARCHAR(255) NOT NULL,
    `product_price` BIGINT NOT NULL,
    `quantity` INT NOT NULL,
    `created_at` DATETIME(6) NOT NULL,
    `modified_at` DATETIME(6) NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_order_item_order_id` (`order_id`),
    KEY `idx_order_item_product_id` (`product_id`),
    CONSTRAINT `fk_order_item_order`
        FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`),
    CONSTRAINT `fk_order_item_product`
        FOREIGN KEY (`product_id`) REFERENCES `products` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 일반 주문과 특가 주문이 같은 숫자 ID를 가질 수 있으므로 결제의 주문 유형을 구분합니다.
-- 기존 결제 데이터는 모두 기존 특가 주문에서 생성된 데이터이므로 PROMOTION으로 보정합니다.
ALTER TABLE `payments`
    ADD COLUMN `order_type` VARCHAR(20) NULL AFTER `order_id`;

UPDATE `payments`
SET `order_type` = 'PROMOTION'
WHERE `order_type` IS NULL;

ALTER TABLE `payments`
    MODIFY COLUMN `order_type` VARCHAR(20) NOT NULL;

-- 주문 유형과 주문 ID를 함께 사용하는 결제 조회를 지원합니다.
CREATE INDEX `idx_payments_order_type_order_id`
    ON `payments` (`order_type`, `order_id`);
