package kr.co.mindrepublic.coupon.presentation.dto

import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive

/**
 * 쿠폰 발급 요청 본문. couponId 는 경로 변수로 받고, 발급 주체만 본문으로 받는다.
 */
data class CouponIssueRequest(
    @field:NotNull(message = "userId 는 필수입니다")
    @field:Positive(message = "userId 는 양수여야 합니다")
    val userId: Long?,
)
