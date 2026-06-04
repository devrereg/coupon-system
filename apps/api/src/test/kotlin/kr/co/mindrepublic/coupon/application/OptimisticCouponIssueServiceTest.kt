package kr.co.mindrepublic.coupon.application

import kr.co.mindrepublic.coupon.domain.coupon.OutOfStockException
import kr.co.mindrepublic.coupon.domain.issue.CouponIssue
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.dao.OptimisticLockingFailureException

/**
 * 낙관 락 재시도 루프(OptimisticCouponIssueService) 단위 테스트.
 *
 * 한 번의 시도(OptimisticCouponIssueAttempt)는 목으로 대체해 루프의 행위만 검증한다.
 * (실제 DB·트랜잭션·버전 충돌은 단위 3 동시성 통합 테스트에서 다룬다.)
 *
 * 재시도 대상 예외 타입: org.springframework.dao.OptimisticLockingFailureException.
 * Spring 의 HibernateJpaDialect 가 커밋/flush 시점의 Hibernate StaleObjectStateException 을
 * 그 하위 타입(ObjectOptimisticLockingFailureException)으로 변환하므로, 상위 타입을 잡으면
 * 어떤 구체 타입이 오든 충돌만 안전하게 한정해 재시도할 수 있다.
 */
class OptimisticCouponIssueServiceTest {

    private val couponId = 1L
    private val userId = 7L

    @Test
    fun `충돌 예외를 N번 던진 뒤 성공하면 N회 재시도 후 성공하고 시도 횟수를 반환한다`() {
        val expected = CouponIssue(couponId = couponId, userId = userId, id = 100L)
        val attempt = mock<OptimisticCouponIssueAttempt> {
            on { attempt(couponId, userId) }
                .doThrow(OptimisticLockingFailureException("conflict #1"))
                .doThrow(OptimisticLockingFailureException("conflict #2"))
                .doThrow(OptimisticLockingFailureException("conflict #3"))
                .doReturn(expected)
        }
        val service = OptimisticCouponIssueService(attempt)

        val result = service.issue(couponId, userId)

        // 3번 충돌 후 4번째 시도에서 성공 → 총 시도 4회
        assertThat(result.issue).isSameAs(expected)
        assertThat(result.attempts).isEqualTo(4)
        verify(attempt, times(4)).attempt(couponId, userId)
    }

    @Test
    fun `OutOfStockException 은 재시도하지 않고 즉시 전파한다`() {
        val attempt = mock<OptimisticCouponIssueAttempt> {
            on { attempt(couponId, userId) } doThrow OutOfStockException()
        }
        val service = OptimisticCouponIssueService(attempt)

        assertThatThrownBy { service.issue(couponId, userId) }
            .isInstanceOf(OutOfStockException::class.java)

        // 비즈니스 실패는 단 한 번만 시도되고 재시도되지 않는다.
        verify(attempt, times(1)).attempt(couponId, userId)
    }

    @Test
    fun `최대 시도 횟수를 초과하면 마지막 충돌 예외를 전파한다`() {
        val last = OptimisticLockingFailureException("final conflict")
        val attempt = mock<OptimisticCouponIssueAttempt> {
            on { attempt(any(), any()) } doThrow last
        }
        val service = OptimisticCouponIssueService(attempt)

        assertThatThrownBy { service.issue(couponId, userId) }
            .isInstanceOf(OptimisticLockingFailureException::class.java)

        // 충돌이 끝까지 계속되면 최대 시도 횟수만큼 호출하고 포기한다.
        verify(attempt, times(OptimisticCouponIssueService.MAX_ATTEMPTS)).attempt(couponId, userId)
    }
}
