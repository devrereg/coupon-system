package kr.co.mindrepublic.coupon.application

import kr.co.mindrepublic.coupon.domain.coupon.StockReservation
import kr.co.mindrepublic.coupon.domain.issue.CouponIssue
import kr.co.mindrepublic.coupon.domain.issue.CouponIssueRequested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.kafka.support.Acknowledgment

/**
 * 컨슈머 보상 분기 단위 테스트 — persist/reservation/ack 를 목으로 대체.
 * unique 위반(중복)=재배달이므로 반납 없이 ack / 그 외 영구 실패=releaseWithDedup 후 ack.
 */
class CouponIssueEventConsumerTest {
    private val couponId = 1L
    private val userId = 7L
    private val event = CouponIssueRequested(couponId, userId)

    @Test
    fun `unique 위반(중복)이면 슬롯을 반납하지 않고 ack 한다`() {
        val persist = mock<RedisCouponIssuePersist> {
            on { persist(couponId, userId) } doThrow DuplicateIssueException(couponId, userId)
        }
        val reservation = mock<StockReservation>()
        val ack = mock<Acknowledgment>()
        val consumer = CouponIssueEventConsumer(persist, reservation)

        consumer.consume(event, ack)

        verify(reservation, never()).releaseWithDedup(any(), any())
        verify(ack, times(1)).acknowledge()
    }

    @Test
    fun `영구 실패면 슬롯을 반납(releaseWithDedup)하고 ack 한다`() {
        val persist = mock<RedisCouponIssuePersist> {
            on { persist(couponId, userId) } doThrow UserNotFoundException(userId)
        }
        val reservation = mock<StockReservation>()
        val ack = mock<Acknowledgment>()
        val consumer = CouponIssueEventConsumer(persist, reservation)

        consumer.consume(event, ack)

        verify(reservation, times(1)).releaseWithDedup(couponId, userId)
        verify(ack, times(1)).acknowledge()
    }

    @Test
    fun `정상 영속이면 반납 없이 ack 한다`() {
        val persist = mock<RedisCouponIssuePersist> {
            on { persist(couponId, userId) } doReturn CouponIssue(couponId = couponId, userId = userId, id = 1L)
        }
        val reservation = mock<StockReservation>()
        val ack = mock<Acknowledgment>()
        val consumer = CouponIssueEventConsumer(persist, reservation)

        consumer.consume(event, ack)

        verify(reservation, never()).releaseWithDedup(any(), any())
        verify(ack, times(1)).acknowledge()
    }
}
