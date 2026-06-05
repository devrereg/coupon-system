package kr.co.mindrepublic.coupon.application

import kr.co.mindrepublic.coupon.domain.coupon.OutOfStockException
import kr.co.mindrepublic.coupon.domain.coupon.StockReservation
import kr.co.mindrepublic.coupon.domain.issue.CouponIssue
import org.springframework.stereotype.Service

/**
 * 쿠폰 발급 유스케이스 — Redis 원자 게이트 경로(Day 6).
 *
 * 이 빈에는 **@Transactional 이 없다.** 트랜잭션 경계는 RedisCouponIssuePersist.persist 에 있고,
 * 보상(release)은 그 트랜잭션이 커밋/롤백된 뒤 **바깥**에서 결과를 보고 결정해야 하기 때문이다.
 *
 * 흐름:
 *  ① reserve(couponId): Redis Lua 로 (검사→차감)을 원자 실행. 잔여 없으면 OutOfStock 즉시(차감 없음 → 보상 불필요).
 *  ② persist(couponId, userId): 별도 빈(프록시 경유)에서 새 트랜잭션으로 발급 이력 저장.
 *  ③ ②가 실패하면 release(couponId) 로 슬롯을 반납하고 예외를 재전파한다 — Redis(권위)와 DB(장부)의 정합성 보상.
 *
 * NOTE: 1인 1매는 DB 권위(존재/중복 확인 + unique)이므로, 중복 요청은 ①게이트를 통과한 뒤 ②에서
 * DuplicateIssueException 으로 떨어진다. 그때 ③ 보상이 슬롯을 되돌려 재고 누수를 막는다.
 */
@Service
class RedisCouponIssueService(
    private val reservation: StockReservation,
    private val persist: RedisCouponIssuePersist,
) {

    fun issue(couponId: Long, userId: Long): CouponIssue {
        if (!reservation.reserve(couponId)) {
            throw OutOfStockException()
        }
        try {
            return persist.persist(couponId, userId)
        } catch (e: Exception) {
            // 게이트는 통과했으나 DB 영속 실패 → 슬롯 반납(보상) 후 재전파.
            reservation.release(couponId)
            throw e
        }
    }
}
