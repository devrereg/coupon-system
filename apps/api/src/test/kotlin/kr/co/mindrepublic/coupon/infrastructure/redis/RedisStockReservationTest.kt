package kr.co.mindrepublic.coupon.infrastructure.redis

import kr.co.mindrepublic.coupon.domain.coupon.StockReservation
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.StringRedisTemplate

/**
 * Redis Lua 게이트 어댑터 통합 테스트.
 * NOTE: Redis 가 떠 있어야 한다(docker compose up -d). 키는 테스트 전용 couponId 로 격리하고 @AfterEach 에서 지운다.
 */
@SpringBootTest
class RedisStockReservationTest @Autowired constructor(
    private val reservation: StockReservation,
    private val redisTemplate: StringRedisTemplate,
) {
    private val couponId = 999_001L

    @AfterEach
    fun tearDown() {
        redisTemplate.delete("coupon:stock:$couponId")
    }

    @Test
    fun `prepare 로 시드한 재고를 reserve 가 원자적으로 차감하고 소진되면 차감 없이 거부한다`() {
        reservation.prepare(couponId, 2)

        assertThat(reservation.reserve(couponId)).isTrue()   // 2 -> 1
        assertThat(reservation.reserve(couponId)).isTrue()   // 1 -> 0
        assertThat(reservation.reserve(couponId)).isFalse()  // 0 -> 거부(차감 안 함)
        assertThat(reservation.remaining(couponId)).isEqualTo(0)  // 음수로 내려가지 않는다
    }

    @Test
    fun `release 는 차감했던 슬롯을 되돌려 다시 발급 가능하게 한다`() {
        reservation.prepare(couponId, 1)
        reservation.reserve(couponId)          // 1 -> 0
        reservation.release(couponId)          // 0 -> 1

        assertThat(reservation.remaining(couponId)).isEqualTo(1)
        assertThat(reservation.reserve(couponId)).isTrue()
    }
}
