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

/**
 * Day 4 — 비관 락이 동시 발급을 직렬화함을 검증하는 회귀 테스트.
 *
 * 재고 100개에 서로 다른 2,000명이 가상 스레드로 동시에 발급을 요청해도,
 * issue() 가 재고를 SELECT … FOR UPDATE 로 조회해 (조회→차감)을 직렬화하므로
 * 정확히 100개만 발급된다. (Day 3 에서는 락이 없어 이 단언이 FAIL 했다.)
 *
 * @DataJpaTest 를 쓰지 않는 이유: @DataJpaTest 는 테스트를 트랜잭션으로 감싸 롤백하므로
 * 스레드별 독립 커밋이 일어나지 않는다. @SpringBootTest 는 테스트 메서드를 트랜잭션으로
 * 감싸지 않아 각 스레드의 issue() 트랜잭션이 독립적으로 커밋된다 → 정리도 @AfterEach 에서 직접 한다.
 *
 * NOTE: 전체 컨텍스트를 로드하므로 PostgreSQL/Redis 가 떠 있어야 한다(docker compose up -d).
 */
@SpringBootTest
class CouponIssueConcurrencyTest @Autowired constructor(
    private val service: CouponIssueService,
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
                name = "Day4 동시성 쿠폰",
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
    fun `재고 100개에 2000명이 동시 발급해도 비관 락으로 정확히 100개만 발급된다`() {
        val success = AtomicInteger(0)
        val fail = AtomicInteger(0)

        // ready/start/done 3단 배리어로 모든 스레드를 같은 순간에 발사한다.
        val ready = CountDownLatch(CONCURRENCY)
        val start = CountDownLatch(1)
        val done = CountDownLatch(CONCURRENCY)

        Executors.newVirtualThreadPerTaskExecutor().use { executor ->
            userIds.forEach { userId ->
                executor.submit {
                    ready.countDown()          // 준비 완료 알림
                    start.await()              // 발사 신호 대기
                    try {
                        service.issue(couponId, userId)
                        success.incrementAndGet()
                    } catch (e: Exception) {
                        // 재고 소진/충돌 등 모든 예외는 실패로 집계
                        fail.incrementAndGet()
                    } finally {
                        done.countDown()
                    }
                }
            }

            ready.await(READY_TIMEOUT_SECONDS, TimeUnit.SECONDS) // 모든 스레드가 출발선에 설 때까지
            start.countDown()                                    // 동시 발사
            check(done.await(DONE_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                "동시 발급이 ${DONE_TIMEOUT_SECONDS}s 안에 끝나지 않았다"
            }
        }

        val successCount = success.get()
        val remaining = couponStockRepository.findByCouponId(couponId)!!.remainingQuantity
        val issued = couponIssueJpaRepository.count()

        // 단언 직전 관찰 로그: 깨짐 크기가 보이도록 성공/실패/잔여/발급행을 함께 출력
        log.warn(
            "동시성 결과 — 성공={} 실패={} 잔여={} 발급행={}",
            successCount, fail.get(), remaining, issued,
        )

        // 비관 락이 동시 차감을 직렬화하므로 불변식이 성립한다.
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
