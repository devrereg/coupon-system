package kr.co.mindrepublic.coupon.infrastructure.redis

import kr.co.mindrepublic.coupon.domain.coupon.ReservationResult
import kr.co.mindrepublic.coupon.domain.coupon.StockReservation
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.StringRedisTemplate

/**
 * Day 7 — 재고+중복 원자 게이트 통합 테스트.
 * NOTE: Redis 가 떠 있어야 한다. 키는 테스트 전용 couponId 로 격리하고 @AfterEach 에서 지운다.
 */
@SpringBootTest
class RedisStockReservationDedupTest @Autowired constructor(
    private val reservation: StockReservation,
    private val redisTemplate: StringRedisTemplate,
) {
    private val couponId = 999_002L

    @AfterEach
    fun tearDown() {
        redisTemplate.delete("coupon:stock:$couponId")
        redisTemplate.delete("coupon:issued:$couponId")
    }

    @Test
    fun `reserveWithDedup 은 재고검사·중복검사·차감을 원자로 수행한다`() {
        reservation.prepare(couponId, 2)

        assertThat(reservation.reserveWithDedup(couponId, 1)).isEqualTo(ReservationResult.RESERVED)    // 2 -> 1
        assertThat(reservation.reserveWithDedup(couponId, 1)).isEqualTo(ReservationResult.DUPLICATE)   // 같은 유저 → 차감 없음
        assertThat(reservation.remaining(couponId)).isEqualTo(1)                                        // 중복은 재고를 안 깎는다
        assertThat(reservation.reserveWithDedup(couponId, 2)).isEqualTo(ReservationResult.RESERVED)    // 1 -> 0
        assertThat(reservation.reserveWithDedup(couponId, 3)).isEqualTo(ReservationResult.OUT_OF_STOCK) // 0 → 소진(차감 없음)
        assertThat(reservation.remaining(couponId)).isEqualTo(0)
    }

    @Test
    fun `releaseWithDedup 은 슬롯과 발급자 표시를 되돌려 재발급 가능하게 한다`() {
        reservation.prepare(couponId, 1)
        reservation.reserveWithDedup(couponId, 1)   // 1 -> 0, issued={1}
        reservation.releaseWithDedup(couponId, 1)   // 0 -> 1, issued={}

        assertThat(reservation.remaining(couponId)).isEqualTo(1)
        assertThat(reservation.reserveWithDedup(couponId, 1)).isEqualTo(ReservationResult.RESERVED) // 표시가 지워져 다시 가능
    }
}
