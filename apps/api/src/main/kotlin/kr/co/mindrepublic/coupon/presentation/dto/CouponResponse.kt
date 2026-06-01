package kr.co.mindrepublic.coupon.presentation.dto

import kr.co.mindrepublic.coupon.application.CouponSummary

/**
 * 쿠폰 조회 응답. 프론트는 remaining(잔여 수량)으로 선착순 UX 를 표시한다.
 */
data class CouponResponse(
    val id: Long,
    val name: String,
    val totalQuantity: Int,
    val remaining: Int,
) {
    companion object {
        fun from(summary: CouponSummary): CouponResponse =
            CouponResponse(
                id = summary.id,
                name = summary.name,
                totalQuantity = summary.totalQuantity,
                remaining = summary.remaining,
            )
    }
}
