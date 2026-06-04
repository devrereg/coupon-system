package kr.co.mindrepublic.coupon.application

import kr.co.mindrepublic.coupon.domain.coupon.CouponStockRepository
import kr.co.mindrepublic.coupon.domain.issue.CouponIssue
import kr.co.mindrepublic.coupon.domain.issue.CouponIssueRepository
import kr.co.mindrepublic.coupon.domain.user.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 쿠폰 발급 "한 번의 시도" — 낙관 락 버전.
 *
 * 흐름은 비관 경로(CouponIssueService.issue)와 같되 재고 조회만 **락 없이**(findByCouponId) 한다.
 * - 두 트랜잭션이 같은 잔여를 읽어 동시에 차감하면, 커밋 시점에
 *   `UPDATE … SET remaining=?, version=v+1 WHERE id=? AND version=v` 의 영향 행이 한쪽은 0이 되어
 *   Hibernate StaleObjectStateException → Spring OptimisticLockingFailureException 으로 드러난다.
 * - 그 충돌의 재시도는 이 메서드 **바깥**(OptimisticCouponIssueService)에서 새 트랜잭션으로 감싼다.
 *   (충돌은 커밋/flush 시점에 터지고 그 순간 트랜잭션이 rollback-only 가 되므로 같은 트랜잭션 안 재시도는 무의미.)
 *
 * @Transactional 이 프록시로 정상 적용되도록 재시도 루프와 **별도 빈**으로 분리했다(자기호출 함정 회피).
 */
@Service
class OptimisticCouponIssueAttempt(
    private val userRepository: UserRepository,
    private val couponStockRepository: CouponStockRepository,
    private val couponIssueRepository: CouponIssueRepository,
) {

    @Transactional
    fun attempt(couponId: Long, userId: Long): CouponIssue {
        // 사용자 존재 확인
        userRepository.findById(userId) ?: throw UserNotFoundException(userId)

        // 1인 1매: 이미 발급받았으면 중복(DB unique 제약과 이중 방어)
        if (couponIssueRepository.existsByCouponIdAndUserId(couponId, userId)) {
            throw DuplicateIssueException(couponId, userId)
        }

        // 재고 조회 (쿠폰당 1행) — 락 없음. 동시성은 @Version 낙관 락 + 바깥 재시도로 보장한다.
        val stock = couponStockRepository.findByCouponId(couponId)
            ?: throw CouponNotFoundException(couponId)

        stock.decrease()

        // 발급 이력 저장
        return couponIssueRepository.save(CouponIssue(couponId = couponId, userId = userId))
    }
}
