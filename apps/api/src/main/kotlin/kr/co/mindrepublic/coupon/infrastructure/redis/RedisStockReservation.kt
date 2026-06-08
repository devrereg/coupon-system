package kr.co.mindrepublic.coupon.infrastructure.redis

import kr.co.mindrepublic.coupon.domain.coupon.ReservationResult
import kr.co.mindrepublic.coupon.domain.coupon.StockReservation
import org.springframework.core.io.ClassPathResource
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.stereotype.Component

/**
 * StockReservation 의 Redis 구현. coupon:stock:{couponId} 카운터를 권위로 삼는다.
 * reserve 는 Lua(reserve_stock.lua)로 (검사→차감)을 원자 실행하고, -1(소진)이면 false 로 매핑한다.
 */
@Component
class RedisStockReservation(
    private val redisTemplate: StringRedisTemplate,
) : StockReservation {

    private val reserveScript = DefaultRedisScript<Long>().apply {
        setLocation(ClassPathResource("redis/reserve_stock.lua"))
        resultType = Long::class.java
    }

    private val reserveWithDedupScript = DefaultRedisScript<Long>().apply {
        setLocation(ClassPathResource("redis/reserve_stock_dedup.lua"))
        resultType = Long::class.java
    }

    override fun prepare(couponId: Long, quantity: Int) {
        redisTemplate.opsForValue().set(key(couponId), quantity.toString())
    }

    override fun reserve(couponId: Long): Boolean {
        val remaining = redisTemplate.execute(reserveScript, listOf(key(couponId)))
        return remaining != null && remaining >= 0
    }

    /**
     * 예약했던 슬롯을 되돌린다(보상). INCR 이므로 **선행 reserve 성공과 짝**일 때만 호출해야 한다 —
     * reserve 없이 부르면 카운터가 시드 수량을 넘어 초과 발급을 허용할 수 있다.
     */
    override fun release(couponId: Long) {
        redisTemplate.opsForValue().increment(key(couponId))
    }

    override fun remaining(couponId: Long): Long =
        redisTemplate.opsForValue().get(key(couponId))?.toLongOrNull() ?: 0

    override fun reserveWithDedup(couponId: Long, userId: Long): ReservationResult {
        val result = redisTemplate.execute(
            reserveWithDedupScript,
            listOf(key(couponId), issuedKey(couponId)),
            userId.toString(),
        )
        return when (result) {
            null, -1L -> ReservationResult.OUT_OF_STOCK   // null 은 방어적으로 통과시키지 않음
            -2L -> ReservationResult.DUPLICATE
            else -> ReservationResult.RESERVED
        }
    }

    override fun releaseWithDedup(couponId: Long, userId: Long) {
        redisTemplate.opsForValue().increment(key(couponId))          // 재고 슬롯 반납
        redisTemplate.opsForSet().remove(issuedKey(couponId), userId.toString())  // 발급자 표시 제거
    }

    private fun key(couponId: Long) = "coupon:stock:$couponId"

    private fun issuedKey(couponId: Long) = "coupon:issued:$couponId"
}
