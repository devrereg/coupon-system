package kr.co.mindrepublic.coupon.application

import kr.co.mindrepublic.coupon.domain.coupon.OutOfStockException
import kr.co.mindrepublic.coupon.domain.coupon.ReservationResult
import kr.co.mindrepublic.coupon.domain.coupon.StockReservation
import kr.co.mindrepublic.coupon.domain.issue.CouponIssueEventPublisher
import kr.co.mindrepublic.coupon.domain.issue.CouponIssueRequested
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
 * RedisKafkaCouponIssueService(조율) 단위 테스트 — 게이트·발행 포트를 목으로 대체해
 * "게이트 3-state 분기"와 "발행 실패 시 보상(releaseWithDedup) 호출 여부"의 행위만 검증한다.
 */
class RedisKafkaCouponIssueServiceTest {
    private val couponId = 1L
    private val userId = 7L

    @Test
    fun `게이트 소진이면 OutOfStock 즉시, 발행·보상을 호출하지 않는다`() {
        val reservation = mock<StockReservation> {
            on { reserveWithDedup(couponId, userId) } doReturn ReservationResult.OUT_OF_STOCK
        }
        val publisher = mock<CouponIssueEventPublisher>()
        val service = RedisKafkaCouponIssueService(reservation, publisher)

        assertThatThrownBy { service.issue(couponId, userId) }
            .isInstanceOf(OutOfStockException::class.java)

        verify(publisher, never()).publish(any())
        verify(reservation, never()).releaseWithDedup(any(), any())
    }

    @Test
    fun `게이트 중복이면 Duplicate 즉시, 발행·보상을 호출하지 않는다`() {
        val reservation = mock<StockReservation> {
            on { reserveWithDedup(couponId, userId) } doReturn ReservationResult.DUPLICATE
        }
        val publisher = mock<CouponIssueEventPublisher>()
        val service = RedisKafkaCouponIssueService(reservation, publisher)

        assertThatThrownBy { service.issue(couponId, userId) }
            .isInstanceOf(DuplicateIssueException::class.java)

        verify(publisher, never()).publish(any())
        verify(reservation, never()).releaseWithDedup(any(), any())
    }

    @Test
    fun `게이트 통과 후 발행이 실패하면 슬롯을 반납하고 예외를 전파한다`() {
        val reservation = mock<StockReservation> {
            on { reserveWithDedup(couponId, userId) } doReturn ReservationResult.RESERVED
        }
        val publisher = mock<CouponIssueEventPublisher> {
            on { publish(any()) } doThrow RuntimeException("kafka down")
        }
        val service = RedisKafkaCouponIssueService(reservation, publisher)

        assertThatThrownBy { service.issue(couponId, userId) }
            .isInstanceOf(RuntimeException::class.java)

        verify(reservation, times(1)).releaseWithDedup(couponId, userId)
    }

    @Test
    fun `정상 발급 시 이벤트를 발행하고 슬롯을 반납하지 않는다`() {
        val reservation = mock<StockReservation> {
            on { reserveWithDedup(couponId, userId) } doReturn ReservationResult.RESERVED
        }
        val publisher = mock<CouponIssueEventPublisher>()
        val service = RedisKafkaCouponIssueService(reservation, publisher)

        service.issue(couponId, userId)

        verify(publisher, times(1)).publish(CouponIssueRequested(couponId, userId))
        verify(reservation, never()).releaseWithDedup(any(), any())
    }

    @Test
    fun `보상(releaseWithDedup)이 실패해도 원래 발행 예외를 잃지 않고 전파하며 release 오류는 suppressed 로 붙는다`() {
        val publishError = RuntimeException("kafka down")
        val releaseError = RuntimeException("redis down")
        val reservation = mock<StockReservation> {
            on { reserveWithDedup(couponId, userId) } doReturn ReservationResult.RESERVED
            on { releaseWithDedup(couponId, userId) } doThrow releaseError
        }
        val publisher = mock<CouponIssueEventPublisher> {
            on { publish(any()) } doThrow publishError
        }
        val service = RedisKafkaCouponIssueService(reservation, publisher)

        assertThatThrownBy { service.issue(couponId, userId) }
            .isSameAs(publishError)
            .satisfies({ thrown -> assertThat(thrown.suppressed).contains(releaseError) })

        verify(reservation, times(1)).releaseWithDedup(couponId, userId)
    }
}
