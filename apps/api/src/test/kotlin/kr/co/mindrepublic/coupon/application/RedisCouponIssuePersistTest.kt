package kr.co.mindrepublic.coupon.application

import kr.co.mindrepublic.coupon.domain.issue.CouponIssue
import kr.co.mindrepublic.coupon.domain.issue.CouponIssueRepository
import kr.co.mindrepublic.coupon.domain.user.User
import kr.co.mindrepublic.coupon.domain.user.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

/**
 * RedisCouponIssuePersist 단위 테스트 — 리포지토리는 목으로 대체.
 * 이 빈은 재고(coupon_stocks)를 건드리지 않는다(권위는 Redis). 사용자/중복 확인 + 저장만 검증한다.
 */
class RedisCouponIssuePersistTest {
    private val couponId = 1L
    private val userId = 7L

    @Test
    fun `사용자가 존재하고 중복이 아니면 발급 이력을 저장해 반환한다`() {
        val saved = CouponIssue(couponId = couponId, userId = userId, id = 100L)
        val userRepository = mock<UserRepository> {
            on { findById(userId) } doReturn User(name = "u", id = userId)
        }
        val couponIssueRepository = mock<CouponIssueRepository> {
            on { existsByCouponIdAndUserId(couponId, userId) } doReturn false
            on { save(any()) } doReturn saved
        }
        val persist = RedisCouponIssuePersist(userRepository, couponIssueRepository)

        val result = persist.persist(couponId, userId)

        assertThat(result).isSameAs(saved)
    }

    @Test
    fun `이미 발급받은 사용자면 DuplicateIssueException 을 던지고 저장하지 않는다`() {
        val userRepository = mock<UserRepository> {
            on { findById(userId) } doReturn User(name = "u", id = userId)
        }
        val couponIssueRepository = mock<CouponIssueRepository> {
            on { existsByCouponIdAndUserId(couponId, userId) } doReturn true
        }
        val persist = RedisCouponIssuePersist(userRepository, couponIssueRepository)

        assertThatThrownBy { persist.persist(couponId, userId) }
            .isInstanceOf(DuplicateIssueException::class.java)
        verify(couponIssueRepository, never()).save(any())
    }

    @Test
    fun `사용자가 없으면 UserNotFoundException 을 던진다`() {
        val userRepository = mock<UserRepository> {
            on { findById(userId) } doReturn null
        }
        val couponIssueRepository = mock<CouponIssueRepository>()
        val persist = RedisCouponIssuePersist(userRepository, couponIssueRepository)

        assertThatThrownBy { persist.persist(couponId, userId) }
            .isInstanceOf(UserNotFoundException::class.java)
        verify(couponIssueRepository, never()).save(any())
    }
}
