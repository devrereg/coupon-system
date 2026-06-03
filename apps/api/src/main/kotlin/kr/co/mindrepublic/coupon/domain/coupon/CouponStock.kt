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
     * NOTE: 이 메서드의 0 미만 금지 불변식은 단일 스레드 기준이다. 동시 발급의 정확성은
     * 서비스가 재고를 비관 락(SELECT … FOR UPDATE)으로 조회해 (조회→차감)을 직렬화함으로써 보장한다.
     */
    fun decrease() {
        if (remainingQuantity <= 0) {
            throw OutOfStockException()
        }
        remainingQuantity -= 1
    }
}
