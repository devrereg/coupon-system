package kr.co.mindrepublic.coupon.presentation

import com.fasterxml.jackson.databind.ObjectMapper
import kr.co.mindrepublic.coupon.domain.coupon.Coupon
import kr.co.mindrepublic.coupon.domain.coupon.CouponRepository
import kr.co.mindrepublic.coupon.domain.coupon.CouponStock
import kr.co.mindrepublic.coupon.domain.coupon.CouponStockRepository
import kr.co.mindrepublic.coupon.domain.user.User
import kr.co.mindrepublic.coupon.domain.user.UserRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 발급 REST API 통합 테스트(MockMvc). 실제 PostgreSQL 사용.
 * 성공(201) / 재고 소진(409) 케이스를 검증한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CouponIssueControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
    private val userRepository: UserRepository,
    private val couponRepository: CouponRepository,
    private val couponStockRepository: CouponStockRepository,
) {

    private fun seedCoupon(remaining: Int): Long {
        val coupon = couponRepository.save(
            Coupon(
                name = "선착순 쿠폰-${System.nanoTime()}",
                totalQuantity = if (remaining > 0) remaining else 1,
                issueStartAt = LocalDateTime.now().minusHours(1),
                issueEndAt = LocalDateTime.now().plusDays(1),
            ),
        )
        couponStockRepository.save(CouponStock(coupon = coupon, remainingQuantity = remaining))
        return coupon.id!!
    }

    private fun seedUser(): Long = userRepository.save(User(name = "테스터-${System.nanoTime()}")).id!!

    @Test
    fun `발급 성공 시 201 과 발급 정보를 반환한다`() {
        val couponId = seedCoupon(remaining = 5)
        val userId = seedUser()
        val body = objectMapper.writeValueAsString(mapOf("userId" to userId))

        mockMvc.post("/api/coupons/$couponId/issue") {
            contentType = MediaType.APPLICATION_JSON
            content = body
        }.andExpect {
            status { isCreated() }
            jsonPath("$.couponId") { value(couponId) }
            jsonPath("$.userId") { value(userId) }
            jsonPath("$.issueId") { exists() }
            jsonPath("$.issuedAt") { exists() }
        }
    }

    @Test
    fun `재고가 소진되면 409 를 반환한다`() {
        val couponId = seedCoupon(remaining = 0)
        val userId = seedUser()
        val body = objectMapper.writeValueAsString(mapOf("userId" to userId))

        mockMvc.post("/api/coupons/$couponId/issue") {
            contentType = MediaType.APPLICATION_JSON
            content = body
        }.andExpect {
            status { isConflict() }
        }
    }
}
