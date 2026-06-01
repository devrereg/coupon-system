package kr.co.mindrepublic.coupon.presentation.dto

import kr.co.mindrepublic.coupon.domain.issue.CouponIssue
import java.time.LocalDateTime

/**
 * 쿠폰 발급 응답. 도메인 엔티티를 직접 노출하지 않고 필요한 필드만 평탄화한다.
 */
data class CouponIssueResponse(
    val issueId: Long,
    val couponId: Long,
    val userId: Long,
    val issuedAt: LocalDateTime,
) {
    companion object {
        fun from(issue: CouponIssue): CouponIssueResponse =
            CouponIssueResponse(
                issueId = requireNotNull(issue.id) { "발급 이력 id 가 없습니다" },
                couponId = issue.couponId,
                userId = issue.userId,
                issuedAt = issue.issuedAt,
            )
    }
}
