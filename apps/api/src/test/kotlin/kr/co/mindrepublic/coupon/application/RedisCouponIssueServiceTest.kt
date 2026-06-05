package kr.co.mindrepublic.coupon.application

import kr.co.mindrepublic.coupon.domain.coupon.OutOfStockException
import kr.co.mindrepublic.coupon.domain.coupon.StockReservation
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

/**
 * RedisCouponIssueService(조율) 단위 테스트 — 게이트와 영속 빈을 목으로 대체해
 * "게이트 통과/거부"와 "보상(release) 호출 여부"의 행위만 검증한다.
 */
class RedisCouponIssueServiceTest {
    private val couponId = 1L
    private val userId = 7L

    @Test
    fun `게이트가 거부하면 OutOfStock 을 던지고 DB 영속과 보상을 호출하지 않는다`() {
        val reservation = mock<StockReservation> { on { reserve(couponId) } doReturn false }
        val persist = mock<RedisCouponIssuePersist>()
        val service = RedisCouponIssueService(reservation, persist)

        assertThatThrownBy { service.issue(couponId, userId) }
            .isInstanceOf(OutOfStockException::class.java)

        verify(persist, never()).persist(any(), any())
        verify(reservation, never()).release(any())
    }

    @Test
    fun `게이트 통과 후 DB 영속이 실패하면 슬롯을 반납(release)하고 예외를 전파한다`() {
        val reservation = mock<StockReservation> { on { reserve(couponId) } doReturn true }
        val persist = mock<RedisCouponIssuePersist> {
            on { persist(couponId, userId) } doThrow DuplicateIssueException(couponId, userId)
        }
        val service = RedisCouponIssueService(reservation, persist)

        assertThatThrownBy { service.issue(couponId, userId) }
            .isInstanceOf(DuplicateIssueException::class.java)

        verify(reservation, times(1)).release(couponId)
    }

    @Test
    fun `정상 발급 시 슬롯을 반납하지 않고 발급 이력을 반환한다`() {
        val expected = CouponIssue(couponId = couponId, userId = userId, id = 100L)
        val reservation = mock<StockReservation> { on { reserve(couponId) } doReturn true }
        val persist = mock<RedisCouponIssuePersist> { on { persist(couponId, userId) } doReturn expected }
        val service = RedisCouponIssueService(reservation, persist)

        val result = service.issue(couponId, userId)

        assertThat(result).isSameAs(expected)
        verify(reservation, never()).release(any())
    }
}
