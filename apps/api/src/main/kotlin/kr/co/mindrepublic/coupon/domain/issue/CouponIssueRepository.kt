package kr.co.mindrepublic.coupon.domain.issue

/**
 * 발급 이력 영속 포트(domain). 구현은 infrastructure 가 제공.
 */
interface CouponIssueRepository {
    fun save(issue: CouponIssue): CouponIssue

    /** 중복 발급 체크용: 해당 쿠폰을 해당 사용자가 이미 발급받았는지. */
    fun existsByCouponIdAndUserId(couponId: Long, userId: Long): Boolean
}
