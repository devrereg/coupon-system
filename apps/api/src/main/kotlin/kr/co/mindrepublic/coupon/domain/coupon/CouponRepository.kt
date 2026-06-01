package kr.co.mindrepublic.coupon.domain.coupon

/**
 * 쿠폰 영속 포트(domain). 구현은 infrastructure 가 제공.
 */
interface CouponRepository {
    fun save(coupon: Coupon): Coupon
    fun findById(id: Long): Coupon?
    fun findAll(): List<Coupon>
}
