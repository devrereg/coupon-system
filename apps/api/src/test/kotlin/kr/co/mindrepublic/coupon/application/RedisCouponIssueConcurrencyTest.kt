package kr.co.mindrepublic.coupon.application

import kr.co.mindrepublic.coupon.domain.coupon.Coupon
import kr.co.mindrepublic.coupon.domain.coupon.CouponStock
import kr.co.mindrepublic.coupon.domain.coupon.StockReservation
import kr.co.mindrepublic.coupon.domain.user.User
import kr.co.mindrepublic.coupon.infrastructure.persistence.CouponIssueJpaRepository
import kr.co.mindrepublic.coupon.infrastructure.persistence.CouponJpaRepository
import kr.co.mindrepublic.coupon.infrastructure.persistence.CouponStockJpaRepository
import kr.co.mindrepublic.coupon.infrastructure.persistence.UserJpaRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.StringRedisTemplate
import java.time.LocalDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Day 6 — Redis 원자 게이트가 동시 발급을 정확히 100개로 수렴시킴을 검증하는 회귀 테스트.
 *
 * 비관(CouponIssueConcurrencyTest)·낙관(OptimisticCouponIssueConcurrencyTest) 테스트를 미러링하되,
 * 호출 대상만 RedisCouponIssueService 로 바꾼다. 비관/낙관은 DB 한 행에서 경합을 풀지만,
 * 이 경로는 재고 권위를 Redis 카운터로 옮겨 DB 행 경합 자체가 없다 — 락 대기도 재시도도 없다.
 *
 * "잔여" 불변식은 DB(coupon_stocks)가 아니라 Redis 에서 읽는다: 권위가 Redis 이고
 * coupon_stocks.remaining 은 요청 경로에서 차감하지 않아 의도적으로 stale 하기 때문이다.
 *
 * NOTE: PostgreSQL/Redis 가 떠 있어야 한다(docker compose up -d).
 */
@SpringBootTest
class RedisCouponIssueConcurrencyTest @Autowired constructor(
    private val service: RedisCouponIssueService,
    private val reservation: StockReservation,
    // 포트에는 키 삭제 수단이 없어 @AfterEach 정리에만 StringRedisTemplate 을 직접 쓴다.
    private val redisTemplate: StringRedisTemplate,
    private val couponJpaRepository: CouponJpaRepository,
    private val couponStockJpaRepository: CouponStockJpaRepository,
    private val userJpaRepository: UserJpaRepository,
    private val couponIssueJpaRepository: CouponIssueJpaRepository,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    private var couponId: Long = 0
    private var userIds: List<Long> = emptyList()

    @BeforeEach
    fun setUp() {
        val coupon = couponJpaRepository.save(
            Coupon(
                name = "Day6 Redis 게이트 동시성 쿠폰",
                totalQuantity = TOTAL_QUANTITY,
                issueStartAt = LocalDateTime.now().minusHours(1),
                issueEndAt = LocalDateTime.now().plusDays(1),
            ),
        )
        // coupon_stocks 행도 만들어 두지만(스키마 일관성), 이 경로의 재고 권위는 Redis 다.
        couponStockJpaRepository.save(CouponStock(coupon = coupon, remainingQuantity = TOTAL_QUANTITY))
        couponId = coupon.id!!

        userIds = userJpaRepository
            .saveAll((1..CONCURRENCY).map { User(name = "user-$it") })
            .map { it.id!! }

        // 재고 권위 = Redis 카운터. 발급 시작 전 100으로 시딩.
        reservation.prepare(couponId, TOTAL_QUANTITY)
    }

    @AfterEach
    fun tearDown() {
        couponIssueJpaRepository.deleteAllInBatch()
        couponStockJpaRepository.deleteAllInBatch()
        couponJpaRepository.deleteAllInBatch()
        userJpaRepository.deleteAllInBatch()
        // Redis 카운터 키를 지워 다음 테스트로 상태가 새지 않게 한다(Day 4 오염 교훈).
        redisTemplate.delete("coupon:stock:$couponId")
    }

    @Test
    fun `재고 100개에 2000명이 동시 발급해도 Redis 게이트로 정확히 100개만 발급된다`() {
        val success = AtomicInteger(0)
        val fail = AtomicInteger(0)

        val ready = CountDownLatch(CONCURRENCY)
        val start = CountDownLatch(1)
        val done = CountDownLatch(CONCURRENCY)

        val elapsedMillis: Long
        Executors.newVirtualThreadPerTaskExecutor().use { executor ->
            userIds.forEach { userId ->
                executor.submit {
                    ready.countDown()
                    start.await()
                    try {
                        service.issue(couponId, userId)
                        success.incrementAndGet()
                    } catch (e: Exception) {
                        fail.incrementAndGet()
                    } finally {
                        done.countDown()
                    }
                }
            }

            ready.await(READY_TIMEOUT_SECONDS, TimeUnit.SECONDS)

            val startedAt = System.nanoTime()
            start.countDown()
            check(done.await(DONE_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                "동시 발급이 ${DONE_TIMEOUT_SECONDS}s 안에 끝나지 않았다"
            }
            elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000
        }

        val successCount = success.get()
        val remaining = reservation.remaining(couponId)               // 권위 = Redis
        val issued = couponIssueJpaRepository.countByCouponId(couponId)

        log.warn(
            "Redis 게이트 동시성 결과 — 성공={} 실패={} Redis잔여={} 발급행={} 소요={}ms",
            successCount, fail.get(), remaining, issued, elapsedMillis,
        )

        assertThat(successCount).isEqualTo(TOTAL_QUANTITY)
        assertThat(remaining).isEqualTo(0L)
        assertThat(issued).isEqualTo(TOTAL_QUANTITY.toLong())
    }

    companion object {
        private const val TOTAL_QUANTITY = 100
        private const val CONCURRENCY = 2_000
        private const val READY_TIMEOUT_SECONDS = 30L
        private const val DONE_TIMEOUT_SECONDS = 60L
    }
}
