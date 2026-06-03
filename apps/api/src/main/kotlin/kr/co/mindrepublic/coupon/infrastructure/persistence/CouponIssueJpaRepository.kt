package kr.co.mindrepublic.coupon.infrastructure.persistence

import kr.co.mindrepublic.coupon.domain.issue.CouponIssue
import org.springframework.data.jpa.repository.JpaRepository

interface CouponIssueJpaRepository : JpaRepository<CouponIssue, Long> {
    fun existsByCouponIdAndUserId(couponId: Long, userId: Long): Boolean
    fun countByCouponId(couponId: Long): Long
}
