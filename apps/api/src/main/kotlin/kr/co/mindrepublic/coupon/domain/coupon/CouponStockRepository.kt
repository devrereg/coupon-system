package kr.co.mindrepublic.coupon.domain.coupon

/**
 * 쿠폰 재고 영속 포트(domain). 구현은 infrastructure 가 제공.
 */
interface CouponStockRepository {
    fun save(stock: CouponStock): CouponStock
    fun findByCouponId(couponId: Long): CouponStock?
}
