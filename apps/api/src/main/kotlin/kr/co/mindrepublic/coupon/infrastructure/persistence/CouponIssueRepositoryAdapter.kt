package kr.co.mindrepublic.coupon.infrastructure.persistence

import kr.co.mindrepublic.coupon.domain.issue.CouponIssue
import kr.co.mindrepublic.coupon.domain.issue.CouponIssueRepository
import org.springframework.stereotype.Repository

@Repository
class CouponIssueRepositoryAdapter(
    private val jpa: CouponIssueJpaRepository,
) : CouponIssueRepository {
    override fun save(issue: CouponIssue): CouponIssue = jpa.save(issue)
    override fun existsByCouponIdAndUserId(couponId: Long, userId: Long): Boolean =
        jpa.existsByCouponIdAndUserId(couponId, userId)
}
