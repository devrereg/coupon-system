package kr.co.mindrepublic.coupon.application

import kr.co.mindrepublic.coupon.domain.coupon.Coupon
import kr.co.mindrepublic.coupon.domain.coupon.CouponStock
import kr.co.mindrepublic.coupon.domain.coupon.CouponStockRepository
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
import java.time.LocalDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Day 5 — 낙관 락 + 수동 재시도가 동시 발급을 정확히 100개로 수렴시킴을 검증하는 회귀 테스트.
 *
 * Day 4 의 비관 락 테스트(CouponIssueConcurrencyTest)를 그대로 미러링하되, 호출 대상만
 * OptimisticCouponIssueService.issue() 로 바꾼다. 비관 경로는 SELECT … FOR UPDATE 로
 * (조회→차감)을 직렬화하는 반면, 낙관 경로는 락 없이 읽고 커밋 시점 @Version 충돌을
 * 바깥 재시도 루프로 흡수한다. 결과 불변식(성공 100 / 잔여 0 / 발급행 100)은 동일하다.
 *
 * 비관 테스트와 달리, 성공한 발급의 (attempts-1) 을 합산해 **총 재시도 횟수**를,
 * start~done 구간의 wall-clock 을 함께 로깅한다 — 비관 vs 낙관 비교 블로그의 근거.
 *
 * @DataJpaTest 가 아닌 @SpringBootTest 를 쓰는 이유는 비관 테스트와 동일하다:
 * 테스트 메서드를 트랜잭션으로 감싸지 않아 각 스레드의 시도가 독립 커밋되고, 정리는 @AfterEach 에서 직접 한다.
 *
 * NOTE: 전체 컨텍스트를 로드하므로 PostgreSQL/Redis 가 떠 있어야 한다(docker compose up -d).
 */
@SpringBootTest
class OptimisticCouponIssueConcurrencyTest @Autowired constructor(
    private val service: OptimisticCouponIssueService,
    private val couponStockRepository: CouponStockRepository,
    // 정리(deleteAll)와 발급행 카운트는 도메인 포트에 수단이 없어 JpaRepository 를 직접 사용한다.
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
        // 재고 100개짜리 선착순 쿠폰
        val coupon = couponJpaRepository.save(
            Coupon(
                name = "Day5 낙관 락 동시성 쿠폰",
                totalQuantity = TOTAL_QUANTITY,
                issueStartAt = LocalDateTime.now().minusHours(1),
                issueEndAt = LocalDateTime.now().plusDays(1),
            ),
        )
        couponStockJpaRepository.save(CouponStock(coupon = coupon, remainingQuantity = TOTAL_QUANTITY))
        couponId = coupon.id!!

        // 1인 1매 unique(coupon_id, user_id) 제약에 막히지 않게 서로 다른 유저 2,000명
        userIds = userJpaRepository
            .saveAll((1..CONCURRENCY).map { User(name = "user-$it") })
            .map { it.id!! }
    }

    @AfterEach
    fun tearDown() {
        // @SpringBootTest 는 커밋된 데이터를 롤백하지 않으므로 직접 삭제한다.
        // FK 순서: couponIssue → stock → coupon, user (자식부터)
        couponIssueJpaRepository.deleteAllInBatch()
        couponStockJpaRepository.deleteAllInBatch()
        couponJpaRepository.deleteAllInBatch()
        userJpaRepository.deleteAllInBatch()
    }

    @Test
    fun `재고 100개에 2000명이 동시 발급해도 낙관 락 재시도로 정확히 100개만 발급된다`() {
        val success = AtomicInteger(0)
        val fail = AtomicInteger(0)
        // 성공한 발급의 (attempts-1) 을 합산한 누적 재시도 횟수 — 낙관 경로의 비용 지표.
        val retries = AtomicLong(0)

        // ready/start/done 3단 배리어로 모든 스레드를 같은 순간에 발사한다.
        val ready = CountDownLatch(CONCURRENCY)
        val start = CountDownLatch(1)
        val done = CountDownLatch(CONCURRENCY)

        val elapsedMillis: Long
        Executors.newVirtualThreadPerTaskExecutor().use { executor ->
            userIds.forEach { userId ->
                executor.submit {
                    ready.countDown()          // 준비 완료 알림
                    start.await()              // 발사 신호 대기
                    try {
                        val result = service.issue(couponId, userId)
                        success.incrementAndGet()
                        // 첫 시도 성공이면 0, 충돌로 재시도했다면 그 횟수만큼 누적.
                        retries.addAndGet((result.attempts - 1).toLong())
                    } catch (e: Exception) {
                        // 재고 소진/충돌 소진 등 모든 예외는 실패로 집계
                        fail.incrementAndGet()
                    } finally {
                        done.countDown()
                    }
                }
            }

            ready.await(READY_TIMEOUT_SECONDS, TimeUnit.SECONDS) // 모든 스레드가 출발선에 설 때까지

            // wall-clock 측정: 동시 발사 직전 ~ 전부 완료 직후
            val startedAt = System.nanoTime()
            start.countDown()                                    // 동시 발사
            check(done.await(DONE_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                "동시 발급이 ${DONE_TIMEOUT_SECONDS}s 안에 끝나지 않았다"
            }
            elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000
        }

        val successCount = success.get()
        val remaining = couponStockRepository.findByCouponId(couponId)!!.remainingQuantity
        val issued = couponIssueJpaRepository.countByCouponId(couponId)

        // 단언 직전 관찰 로그: 비관 경로와 비교할 수 있게 총 재시도 횟수와 소요 시간을 함께 출력
        log.warn(
            "낙관 락 동시성 결과 — 성공={} 실패={} 잔여={} 발급행={} 총재시도={} 소요={}ms",
            successCount, fail.get(), remaining, issued, retries.get(), elapsedMillis,
        )

        // 낙관 락 + 재시도가 동시 차감을 수렴시키므로 비관 경로와 같은 불변식이 성립한다.
        assertThat(successCount).isEqualTo(TOTAL_QUANTITY)
        assertThat(remaining).isEqualTo(0)
        assertThat(issued).isEqualTo(TOTAL_QUANTITY.toLong())
    }

    companion object {
        private const val TOTAL_QUANTITY = 100
        private const val CONCURRENCY = 2_000
        private const val READY_TIMEOUT_SECONDS = 30L
        private const val DONE_TIMEOUT_SECONDS = 60L
    }
}
