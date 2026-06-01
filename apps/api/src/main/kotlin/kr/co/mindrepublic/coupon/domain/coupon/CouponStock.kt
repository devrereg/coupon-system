package kr.co.mindrepublic.coupon.domain.coupon

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table

/**
 * 쿠폰 재고: 쿠폰당 1행으로 잔여 수량을 추적한다.
 * 재고 차감(decrease)의 "0 미만 금지" 불변식을 엔티티 메서드로 소유한다.
 *
 * 별도 엔티티로 둔 이유: Day 4에서 행 단위 락(비관/낙관)의 "락 대상 row"가 명확해진다.
 */
@Entity
@Table(name = "coupon_stocks")
class CouponStock(
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", nullable = false, unique = true)
    val coupon: Coupon,

    @Column(name = "remaining_quantity", nullable = false)
    var remainingQuantity: Int,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long? = null,
) {
    init {
        require(remainingQuantity >= 0) { "잔여 수량은 0 이상이어야 한다: $remainingQuantity" }
    }

    /**
     * 재고 1개 차감. 잔여가 0이면 OutOfStockException.
     *
     * NOTE(Day 3): 이 메서드 자체는 단일 스레드에서 정확하다. 하지만 서비스가 락 없이
     * (조회→확인→차감)을 수행하면, 동시에 여러 트랜잭션이 같은 remainingQuantity 값을
     * 읽고 각자 차감해 lost update 가 발생한다. 그 깨짐을 Day 3 동시성 테스트로 재현한다.
     */
    fun decrease() {
        if (remainingQuantity <= 0) {
            throw OutOfStockException()
        }
        remainingQuantity -= 1
    }
}
