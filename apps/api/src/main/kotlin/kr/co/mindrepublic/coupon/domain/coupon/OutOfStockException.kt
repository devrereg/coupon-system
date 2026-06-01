package kr.co.mindrepublic.coupon.domain.coupon

/**
 * 잔여 재고가 없는데 차감을 시도하면 던지는 도메인 예외.
 */
class OutOfStockException(message: String = "쿠폰 재고가 소진되었습니다") : RuntimeException(message)
