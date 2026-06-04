package kr.co.mindrepublic.coupon.application

import kr.co.mindrepublic.coupon.domain.issue.CouponIssue
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.stereotype.Service

/**
 * 낙관 락 발급 결과. 발급 이력과 함께 총 시도 횟수(attempts)를 노출한다.
 * (성공한 시도까지 포함한 누적 횟수 — 단위 3 동시성 테스트가 합산해 재시도 비용을 측정한다.)
 */
data class OptimisticIssueResult(
    val issue: CouponIssue,
    val attempts: Int,
)

/**
 * 쿠폰 발급 유스케이스 — 낙관 락 + 수동 재시도 루프.
 *
 * 이 빈에는 **@Transactional 이 없다.** 트랜잭션 경계는 매 시도(OptimisticCouponIssueAttempt.attempt)에 있고,
 * 재시도는 그 트랜잭션 **바깥**에서 매번 새 트랜잭션·새 영속성 컨텍스트로 다시 시도해야 하기 때문이다.
 *
 * attempt 를 **생성자 주입**(별도 빈)으로 받아 호출하므로 프록시를 경유해 @Transactional 이 정상 적용된다.
 * (같은 빈 안에서 this.attempt() 를 부르면 프록시를 건너뛰어 트랜잭션이 무시되는 자기호출 함정에 빠진다.)
 *
 * 재시도 대상은 **낙관 락 충돌(OptimisticLockingFailureException)로만 한정**한다.
 * OutOfStock·Duplicate·UserNotFound 같은 정당한 비즈니스 실패는 재시도하지 않고 즉시 전파한다 —
 * 재시도해도 결과가 바뀌지 않으며, 재시도 대상을 충돌로만 좁히는 것이 정확성의 핵심이다.
 */
@Service
class OptimisticCouponIssueService(
    private val attempt: OptimisticCouponIssueAttempt,
) {

    fun issue(couponId: Long, userId: Long): OptimisticIssueResult {
        var lastConflict: OptimisticLockingFailureException? = null

        for (attemptNo in 1..MAX_ATTEMPTS) {
            try {
                val issue = attempt.attempt(couponId, userId)
                return OptimisticIssueResult(issue = issue, attempts = attemptNo)
            } catch (e: OptimisticLockingFailureException) {
                // 충돌만 재시도 — 다음 시도는 새 트랜잭션에서 최신 버전을 다시 읽는다.
                lastConflict = e
            }
        }

        // 최대 시도 초과: 마지막 충돌 예외를 전파한다.
        throw lastConflict!!
    }

    companion object {
        /**
         * 최대 시도 횟수. 단위 2 에서는 충분히 크게 잡아 두고, 단위 3 동시성 테스트(2,000 경쟁)로 튜닝한다.
         */
        const val MAX_ATTEMPTS = 100
    }
}
