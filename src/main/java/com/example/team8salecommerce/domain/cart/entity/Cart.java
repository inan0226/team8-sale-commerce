package com.example.team8salecommerce.domain.cart.entity;


import com.example.team8salecommerce.domain.member.entity.Member;
import com.example.team8salecommerce.global.util.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 회원당 하나의 장바구니 소유
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "cart")
public class Cart extends BaseEntity {

    // 장바구니 id
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 장바구니 소유 회원
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "member_id",
            nullable = false,
            unique = true
    )
    private Member member;

    // 장바구니 생성
    public Cart(Member member) {
        this.member = member;
    }
}
