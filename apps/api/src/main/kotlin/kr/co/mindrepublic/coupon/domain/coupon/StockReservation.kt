package kr.co.mindrepublic.coupon.domain.coupon

/**
 * 재고 예약 포트(domain). 재고의 "권위"를 DB 행 밖(인메모리 카운터)에 두는 발급 경로(Day 6)의 추상화.
 * 구현(infrastructure)은 원자 연산으로 (검사→차감)을 한 번에 수행해 핫 로우 경합을 없앤다.
 */
interface StockReservation {
    /** 발급 시작 전 카운터를 quantity 로 초기화(시딩)한다. */
    fun prepare(couponId: Long, quantity: Int)

    /** 재고 1단위를 원자적으로 예약. 성공하면 true, 잔여가 없으면(차감 없이) false. */
    fun reserve(couponId: Long): Boolean

    /** 예약했던 1단위를 되돌린다(보상). DB 영속 실패 시 슬롯 반납에 쓴다. */
    fun release(couponId: Long)

    /** 현재 잔여 수량. 미초기화면 0. (동시성 테스트 불변식 검증용) */
    fun remaining(couponId: Long): Long
}
