package kr.co.mindrepublic.coupon.presentation

import jakarta.validation.Valid
import kr.co.mindrepublic.coupon.application.CouponIssueService
import kr.co.mindrepublic.coupon.presentation.dto.CouponIssueRequest
import kr.co.mindrepublic.coupon.presentation.dto.CouponIssueResponse
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * 쿠폰 발급 API. 컨트롤러는 얇게: 검증된 요청을 서비스에 위임하고 응답 DTO 로 변환만.
 */
@RestController
@RequestMapping("/api/coupons")
class CouponIssueController(
    private val couponIssueService: CouponIssueService,
) {

    @PostMapping("/{couponId}/issue")
    @ResponseStatus(HttpStatus.CREATED)
    fun issue(
        @PathVariable couponId: Long,
        @Valid @RequestBody request: CouponIssueRequest,
    ): CouponIssueResponse {
        val issue = couponIssueService.issue(couponId, request.userId!!)
        return CouponIssueResponse.from(issue)
    }
}
