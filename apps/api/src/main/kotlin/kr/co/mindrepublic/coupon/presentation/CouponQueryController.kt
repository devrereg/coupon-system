package kr.co.mindrepublic.coupon.presentation

import kr.co.mindrepublic.coupon.application.CouponQueryService
import kr.co.mindrepublic.coupon.presentation.dto.CouponResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 쿠폰 조회 API. 컨트롤러는 얇게: 유스케이스 결과를 응답 DTO 로 변환만.
 */
@RestController
@RequestMapping("/api/coupons")
class CouponQueryController(
    private val couponQueryService: CouponQueryService,
) {

    @GetMapping
    fun list(): List<CouponResponse> =
        couponQueryService.findAll().map(CouponResponse::from)
}
