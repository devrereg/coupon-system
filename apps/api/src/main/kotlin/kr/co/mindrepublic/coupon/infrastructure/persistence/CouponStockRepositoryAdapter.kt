package kr.co.mindrepublic.coupon.infrastructure.persistence

import kr.co.mindrepublic.coupon.domain.coupon.CouponStock
import kr.co.mindrepublic.coupon.domain.coupon.CouponStockRepository
import org.springframework.stereotype.Repository

@Repository
class CouponStockRepositoryAdapter(
    private val jpa: CouponStockJpaRepository,
) : CouponStockRepository {
    override fun save(stock: CouponStock): CouponStock = jpa.save(stock)
    override fun findByCouponId(couponId: Long): CouponStock? = jpa.findByCouponId(couponId)
    override fun findByCouponIdForUpdate(couponId: Long): CouponStock? =
        jpa.findByCouponIdForUpdate(couponId)
}
