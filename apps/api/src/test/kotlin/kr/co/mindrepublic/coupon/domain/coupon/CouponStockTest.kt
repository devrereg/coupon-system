package kr.co.mindrepublic.coupon.domain.coupon

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

/**
 * CouponStock 의 재고 차감 불변식(0 미만 금지)을 도메인 메서드로 검증한다.
 * 순수 단위 테스트 — 스프링 컨텍스트/DB 불필요.
 */
class CouponStockTest {

    @Test
    fun `재고를 차감하면 잔여 수량이 1 줄어든다`() {
        val stock = CouponStock(coupon = newCoupon(), remainingQuantity = 10)

        stock.decrease()

        assertThat(stock.remainingQuantity).isEqualTo(9)
    }

    @Test
    fun `잔여 수량이 0이면 차감 시 예외가 발생한다`() {
        val stock = CouponStock(coupon = newCoupon(), remainingQuantity = 0)

        assertThatThrownBy { stock.decrease() }
            .isInstanceOf(OutOfStockException::class.java)
    }

    private fun newCoupon(): Coupon =
        Coupon(
            name = "테스트 쿠폰",
            totalQuantity = 10,
            issueStartAt = java.time.LocalDateTime.now().minusDays(1),
            issueEndAt = java.time.LocalDateTime.now().plusDays(1),
        )
}
