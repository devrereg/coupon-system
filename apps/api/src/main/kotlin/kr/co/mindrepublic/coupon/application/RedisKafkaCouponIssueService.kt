package kr.co.mindrepublic.coupon.application

import kr.co.mindrepublic.coupon.domain.coupon.OutOfStockException
import kr.co.mindrepublic.coupon.domain.coupon.ReservationResult
import kr.co.mindrepublic.coupon.domain.coupon.StockReservation
import kr.co.mindrepublic.coupon.domain.issue.CouponIssueEventPublisher
import kr.co.mindrepublic.coupon.domain.issue.CouponIssueRequested
import org.springframework.stereotype.Service

/**
 * 쿠폰 발급 유스케이스 — Redis 게이트 + Kafka 비동기 경로(Day 7).
 *
 * @Transactional 이 없다. DB 영속은 이 경로에 없고(컨슈머가 한다), 발행 실패 보상도
 * 트랜잭션과 무관한 Redis 카운터 되돌리기이기 때문이다.
 *
 * 흐름:
 *  ① reserveWithDedup: Lua 로 (재고검사→중복검사→차감)을 원자 실행. 소진/중복은 차감 없이 즉시 거절(보상 불필요).
 *  ② publish: 발급 의도를 Kafka 로 동기 발행(acks=all). 이후 DB 영속은 컨슈머가 나중에 처리.
 *  ③ ②가 실패하면 releaseWithDedup 로 슬롯+발급자 표시를 되돌리고 예외를 재전파(동기 보상).
 */
@Service
class RedisKafkaCouponIssueService(
    private val reservation: StockReservation,
    private val publisher: CouponIssueEventPublisher,
) {

    fun issue(couponId: Long, userId: Long) {
        when (reservation.reserveWithDedup(couponId, userId)) {
            ReservationResult.OUT_OF_STOCK -> throw OutOfStockException()
            ReservationResult.DUPLICATE -> throw DuplicateIssueException(couponId, userId)
            ReservationResult.RESERVED -> Unit
        }
        try {
            publisher.publish(CouponIssueRequested(couponId, userId))
        } catch (e: Exception) {
            // 게이트는 통과했으나 발행 실패 → 슬롯 반납(보상) 후 재전파.
            // release 자체가 실패해도 원래 발행 예외를 잃지 않도록 suppressed 로 붙인다(Day 6과 동일).
            try {
                reservation.releaseWithDedup(couponId, userId)
            } catch (releaseError: Exception) {
                e.addSuppressed(releaseError)
            }
            throw e
        }
    }
}
