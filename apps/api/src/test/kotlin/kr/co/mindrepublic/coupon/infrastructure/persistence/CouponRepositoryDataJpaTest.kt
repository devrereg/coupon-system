package kr.co.mindrepublic.coupon.infrastructure.persistence

import kr.co.mindrepublic.coupon.domain.coupon.Coupon
import kr.co.mindrepublic.coupon.domain.coupon.CouponRepository
import kr.co.mindrepublic.coupon.domain.coupon.CouponStock
import kr.co.mindrepublic.coupon.domain.coupon.CouponStockRepository
import kr.co.mindrepublic.coupon.domain.issue.CouponIssue
import kr.co.mindrepublic.coupon.domain.issue.CouponIssueRepository
import kr.co.mindrepublic.coupon.domain.user.User
import kr.co.mindrepublic.coupon.domain.user.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import java.time.LocalDateTime

/**
 * 리포지토리 어댑터 슬라이스 테스트.
 *
 * 실제 PostgreSQL(15432) + Flyway 마이그레이션으로 검증한다. 따라서 임베디드 DB 치환을 끄고
 * (replace = NONE), domain 포트의 어댑터들을 @Import 로 컨텍스트에 올린다.
 * 인프라(docker compose up -d)가 떠 있어야 통과한다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(
    UserRepositoryAdapter::class,
    CouponRepositoryAdapter::class,
    CouponStockRepositoryAdapter::class,
    CouponIssueRepositoryAdapter::class,
)
class CouponRepositoryDataJpaTest @Autowired constructor(
    private val userRepository: UserRepository,
    private val couponRepository: CouponRepository,
    private val couponStockRepository: CouponStockRepository,
    private val couponIssueRepository: CouponIssueRepository,
) {

    @Test
    fun `쿠폰과 재고를 저장하고 couponId 로 재고를 조회한다`() {
        val coupon = couponRepository.save(newCoupon())
        couponStockRepository.save(CouponStock(coupon = coupon, remainingQuantity = 100))

        val found = couponStockRepository.findByCouponId(coupon.id!!)

        assertThat(found).isNotNull
        assertThat(found!!.remainingQuantity).isEqualTo(100)
    }

    @Test
    fun `couponId 로 재고를 비관 락으로 조회한다`() {
        val coupon = couponRepository.save(newCoupon())
        couponStockRepository.save(CouponStock(coupon = coupon, remainingQuantity = 100))

        val found = couponStockRepository.findByCouponIdForUpdate(coupon.id!!)

        assertThat(found).isNotNull
        assertThat(found!!.remainingQuantity).isEqualTo(100)
    }

    @Test
    fun `발급 이력 저장 후 중복 발급 여부를 조회한다`() {
        val coupon = couponRepository.save(newCoupon())
        val user = userRepository.save(User(name = "홍길동"))

        assertThat(couponIssueRepository.existsByCouponIdAndUserId(coupon.id!!, user.id!!)).isFalse

        couponIssueRepository.save(CouponIssue(couponId = coupon.id!!, userId = user.id!!))

        assertThat(couponIssueRepository.existsByCouponIdAndUserId(coupon.id!!, user.id!!)).isTrue
    }

    private fun newCoupon(): Coupon =
        Coupon(
            name = "선착순 쿠폰",
            totalQuantity = 100,
            issueStartAt = LocalDateTime.now().minusHours(1),
            issueEndAt = LocalDateTime.now().plusDays(1),
        )
}
