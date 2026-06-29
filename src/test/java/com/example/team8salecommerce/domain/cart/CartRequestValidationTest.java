package com.example.team8salecommerce.domain.cart;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.team8salecommerce.domain.cart.dto.request.AddCartItemRequest;
import com.example.team8salecommerce.domain.cart.dto.request.UpdateCartItemRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 장바구니 요청 DTO의 필수 수량 검증을 확인한다.
 */
class CartRequestValidationTest {

    /** 검증기 생명주기를 관리하는 팩토리 */
    private static ValidatorFactory validatorFactory;

    /** DTO Bean Validation 검증기 */
    private static Validator validator;

    /**
     * 테스트 전체에서 사용할 검증기를 생성한다.
     */
    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    /**
     * 테스트 종료 후 검증기 자원을 해제한다.
     */
    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    @DisplayName("상품 추가 요청에서 수량이 누락되면 검증에 실패한다")
    void addCartItemQuantityIsRequired() {
        // given: 수량이 누락된 상품 추가 요청을 준비한다.
        AddCartItemRequest request = new AddCartItemRequest(1L, null);

        // when & then: quantity 필드의 필수값 위반을 확인한다.
        assertThat(validator.validate(request))
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("quantity")
                        && violation.getMessage().equals("수량은 필수입니다."));
    }

    @Test
    @DisplayName("수량 변경 요청에서 수량이 누락되면 검증에 실패한다")
    void updateCartItemQuantityIsRequired() {
        // given: 수량이 누락된 변경 요청을 준비한다.
        UpdateCartItemRequest request = new UpdateCartItemRequest(null);

        // when & then: quantity 필드의 필수값 위반을 확인한다.
        assertThat(validator.validate(request))
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("quantity")
                        && violation.getMessage().equals("수량은 필수입니다."));
    }
}
