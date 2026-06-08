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
 * Day 7 — Redis 게이트 + Kafka 비동기 경로가 동시 발급을 정확히 100개로 수렴시킴을 검증.
 *
 * Day 6(RedisCouponIssueConcurrencyTest)을 미러링하되 호출 대상을 RedisKafkaCouponIssueService 로 바꾼다.
 * 비동기라 발행(접수) 직후엔 DB 발급행이 없으므로, 컨슈머가 다 드레인할 때까지 기다린 뒤 발급행을 단언한다(결과적 일관성).
 *
 * NOTE: PostgreSQL/Redis/Kafka 가 떠 있어야 한다(docker compose up -d).
 */
@SpringBootTest
class RedisKafkaCouponIssueConcurrencyTest @Autowired constructor(
    private val service: RedisKafkaCouponIssueService,
    private val reservation: StockReservation,
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
                name = "Day7 Redis+Kafka 동시성 쿠폰",
                totalQuantity = TOTAL_QUANTITY,
                issueStartAt = LocalDateTime.now().minusHours(1),
                issueEndAt = LocalDateTime.now().plusDays(1),
            ),
        )
        couponStockJpaRepository.save(CouponStock(coupon = coupon, remainingQuantity = TOTAL_QUANTITY))
        couponId = coupon.id!!

        userIds = userJpaRepository
            .saveAll((1..CONCURRENCY).map { User(name = "user-$it") })
            .map { it.id!! }

        reservation.prepare(couponId, TOTAL_QUANTITY)   // 재고 권위 = Redis 카운터
    }

    @AfterEach
    fun tearDown() {
        couponIssueJpaRepository.deleteAllInBatch()
        couponStockJpaRepository.deleteAllInBatch()
        couponJpaRepository.deleteAllInBatch()
        userJpaRepository.deleteAllInBatch()
        // 주의: 공유 컨슈머 그룹(coupon-issue-consumer)의 오프셋은 여기서 못 지운다.
        // 이전 실패 run 의 잔여 이벤트는 옛 couponId 를 달고 있어(매 run 새 couponId — DB identity 단조 증가)
        // 이 run 의 발급행 카운트를 오염시키지 않는다. 이 격리는 couponId 단조성에 의존한다.
        redisTemplate.delete("coupon:stock:$couponId")
        redisTemplate.delete("coupon:issued:$couponId")
    }

    @Test
    fun `재고 100개에 2000명이 동시 발급하면 게이트 통과 100 발행 후 드레인되면 발급행 100`() {
        val success = AtomicInteger(0)   // 게이트 통과 + 발행 성공(=접수)
        val fail = AtomicInteger(0)      // 소진/중복 거절

        val ready = CountDownLatch(CONCURRENCY)
        val start = CountDownLatch(1)
        val done = CountDownLatch(CONCURRENCY)

        val submittedMillis: Long
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
                "동시 발행이 ${DONE_TIMEOUT_SECONDS}s 안에 끝나지 않았다"
            }
            submittedMillis = (System.nanoTime() - startedAt) / 1_000_000

            // 결과적 일관성: 컨슈머가 발급행 100개를 다 만들 때까지 대기(드레인).
            val drainDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(DRAIN_TIMEOUT_SECONDS)
            while (couponIssueJpaRepository.countByCouponId(couponId) < TOTAL_QUANTITY.toLong() &&
                System.nanoTime() < drainDeadline
            ) {
                Thread.sleep(100)
            }
            val drainMillis = (System.nanoTime() - startedAt) / 1_000_000
            log.warn("Redis+Kafka 드레인 완료까지={}ms", drainMillis)

            check(couponIssueJpaRepository.countByCouponId(couponId) >= TOTAL_QUANTITY.toLong()) {
                "드레인 타임아웃: ${DRAIN_TIMEOUT_SECONDS}s 안에 발급행이 " +
                    "${couponIssueJpaRepository.countByCouponId(couponId)}/$TOTAL_QUANTITY 만 채워졌다 " +
                    "(컨슈머 지연/장애 의심 — 단순 타임아웃 상향 말고 원인 확인)"
            }
        }

        val successCount = success.get()
        val remaining = reservation.remaining(couponId)            // 권위 = Redis
        val issued = couponIssueJpaRepository.countByCouponId(couponId)

        log.warn(
            "Redis+Kafka 동시성 결과 — 게이트통과(접수)={} 거절={} Redis잔여={} 발급행={} 접수까지={}ms",
            successCount, fail.get(), remaining, issued, submittedMillis,
        )

        assertThat(successCount).isEqualTo(TOTAL_QUANTITY)              // 통과+발행 == 100
        assertThat(fail.get()).isEqualTo(CONCURRENCY - TOTAL_QUANTITY)  // 거절 == 1900
        assertThat(remaining).isEqualTo(0L)                            // Redis 재고 소진
        assertThat(issued).isEqualTo(TOTAL_QUANTITY.toLong())          // 드레인 후 발급행 100
    }

    companion object {
        private const val TOTAL_QUANTITY = 100
        private const val CONCURRENCY = 2_000
        private const val READY_TIMEOUT_SECONDS = 30L
        private const val DONE_TIMEOUT_SECONDS = 60L
        private const val DRAIN_TIMEOUT_SECONDS = 30L
    }
}
