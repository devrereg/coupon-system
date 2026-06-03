package kr.co.mindrepublic.coupon.infrastructure.persistence

import jakarta.persistence.LockModeType
import kr.co.mindrepublic.coupon.domain.coupon.CouponStock
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query

interface CouponStockJpaRepository : JpaRepository<CouponStock, Long> {
    fun findByCouponId(couponId: Long): CouponStock?

    // SELECT … FOR UPDATE: 해당 재고 row 에 비관적 쓰기 락
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from CouponStock s where s.coupon.id = :couponId")
    fun findByCouponIdForUpdate(couponId: Long): CouponStock?
}
