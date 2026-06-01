package kr.co.mindrepublic.coupon.presentation

import kr.co.mindrepublic.coupon.domain.coupon.Coupon
import kr.co.mindrepublic.coupon.domain.coupon.CouponRepository
import kr.co.mindrepublic.coupon.domain.coupon.CouponStock
import kr.co.mindrepublic.coupon.domain.coupon.CouponStockRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.time.LocalDateTime

/**
 * 쿠폰 조회 REST API 통합 테스트(MockMvc). 실제 PostgreSQL 사용.
 * 쿠폰+재고를 조합한 응답 DTO(id/name/totalQuantity/remaining)를 검증한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CouponQueryControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val couponRepository: CouponRepository,
    private val couponStockRepository: CouponStockRepository,
) {

    @Test
    fun `GET coupons 는 쿠폰과 재고를 조합해 잔여 수량과 함께 반환한다`() {
        val name = "조회용 쿠폰-${System.nanoTime()}"
        val coupon = couponRepository.save(
            Coupon(
                name = name,
                totalQuantity = 100,
                issueStartAt = LocalDateTime.now().minusHours(1),
                issueEndAt = LocalDateTime.now().plusDays(1),
            ),
        )
        couponStockRepository.save(CouponStock(coupon = coupon, remainingQuantity = 42))
        val couponId = coupon.id!!

        mockMvc.get("/api/coupons").andExpect {
            status { isOk() }
            jsonPath("$[?(@.id == $couponId)].name") { value(name) }
            jsonPath("$[?(@.id == $couponId)].totalQuantity") { value(100) }
            jsonPath("$[?(@.id == $couponId)].remaining") { value(42) }
        }
    }
}
