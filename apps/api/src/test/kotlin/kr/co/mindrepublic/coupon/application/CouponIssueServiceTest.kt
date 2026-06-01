package kr.co.mindrepublic.coupon.application

import kr.co.mindrepublic.coupon.domain.coupon.Coupon
import kr.co.mindrepublic.coupon.domain.coupon.CouponRepository
import kr.co.mindrepublic.coupon.domain.coupon.CouponStock
import kr.co.mindrepublic.coupon.domain.coupon.CouponStockRepository
import kr.co.mindrepublic.coupon.domain.coupon.OutOfStockException
import kr.co.mindrepublic.coupon.domain.issue.CouponIssueRepository
import kr.co.mindrepublic.coupon.domain.user.User
import kr.co.mindrepublic.coupon.domain.user.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import kr.co.mindrepublic.coupon.infrastructure.persistence.CouponIssueRepositoryAdapter
import kr.co.mindrepublic.coupon.infrastructure.persistence.CouponRepositoryAdapter
import kr.co.mindrepublic.coupon.infrastructure.persistence.CouponStockRepositoryAdapter
import kr.co.mindrepublic.coupon.infrastructure.persistence.UserRepositoryAdapter
import java.time.LocalDateTime

/**
 * 발급 서비스 단일 스레드 시나리오. 실제 PostgreSQL + 트랜잭션으로 검증한다.
 * (동시성 깨짐은 Day 3에서 별도 다중 스레드 테스트로 재현 예정)
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(
    UserRepositoryAdapter::class,
    CouponRepositoryAdapter::class,
    CouponStockRepositoryAdapter::class,
    CouponIssueRepositoryAdapter::class,
    CouponIssueService::class,
)
class CouponIssueServiceTest @Autowired constructor(
    private val service: CouponIssueService,
    private val userRepository: UserRepository,
    private val couponRepository: CouponRepository,
    private val couponStockRepository: CouponStockRepository,
    private val couponIssueRepository: CouponIssueRepository,
) {

    private var couponId: Long = 0
    private var userId: Long = 0

    @BeforeEach
    fun setUp() {
        val coupon = couponRepository.save(
            Coupon(
                name = "선착순 쿠폰",
                totalQuantity = 1,
                issueStartAt = LocalDateTime.now().minusHours(1),
                issueEndAt = LocalDateTime.now().plusDays(1),
            ),
        )
        couponStockRepository.save(CouponStock(coupon = coupon, remainingQuantity = 1))
        val user = userRepository.save(User(name = "홍길동"))
        couponId = coupon.id!!
        userId = user.id!!
    }

    @Test
    fun `정상 발급 시 재고가 1 줄고 발급 이력이 생긴다`() {
        service.issue(couponId, userId)

        val stock = couponStockRepository.findByCouponId(couponId)!!
        assertThat(stock.remainingQuantity).isEqualTo(0)
        assertThat(couponIssueRepository.existsByCouponIdAndUserId(couponId, userId)).isTrue
    }

    @Test
    fun `재고가 소진되면 OutOfStockException 이 발생한다`() {
        // 다른 사용자가 마지막 1장을 가져간 상황
        val other = userRepository.save(User(name = "김철수"))
        service.issue(couponId, other.id!!)

        assertThatThrownBy { service.issue(couponId, userId) }
            .isInstanceOf(OutOfStockException::class.java)
    }
}
