package kr.co.mindrepublic.coupon.infrastructure.redis

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

    override fun prepare(couponId: Long, quantity: Int) {
        redisTemplate.opsForValue().set(key(couponId), quantity.toString())
    }

    override fun reserve(couponId: Long): Boolean {
        val remaining = redisTemplate.execute(reserveScript, listOf(key(couponId)))
        return remaining != null && remaining >= 0
    }

    override fun release(couponId: Long) {
        redisTemplate.opsForValue().increment(key(couponId))
    }

    override fun remaining(couponId: Long): Long =
        redisTemplate.opsForValue().get(key(couponId))?.toLong() ?: 0

    private fun key(couponId: Long) = "coupon:stock:$couponId"
}
