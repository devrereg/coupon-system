package kr.co.mindrepublic.coupon.infrastructure.persistence

import kr.co.mindrepublic.coupon.domain.coupon.Coupon
import kr.co.mindrepublic.coupon.domain.coupon.CouponRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

@Repository
class CouponRepositoryAdapter(
    private val jpa: CouponJpaRepository,
) : CouponRepository {
    override fun save(coupon: Coupon): Coupon = jpa.save(coupon)
    override fun findById(id: Long): Coupon? = jpa.findByIdOrNull(id)
    override fun findAll(): List<Coupon> = jpa.findAll()
}
