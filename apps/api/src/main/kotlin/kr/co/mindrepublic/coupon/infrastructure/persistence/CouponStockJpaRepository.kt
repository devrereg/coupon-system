package kr.co.mindrepublic.coupon.infrastructure.persistence

import kr.co.mindrepublic.coupon.domain.coupon.CouponStock
import org.springframework.data.jpa.repository.JpaRepository

interface CouponStockJpaRepository : JpaRepository<CouponStock, Long> {
    fun findByCouponId(couponId: Long): CouponStock?
}
