package kr.co.mindrepublic.coupon.application

import kr.co.mindrepublic.coupon.domain.coupon.CouponStockRepository
import kr.co.mindrepublic.coupon.domain.issue.CouponIssue
import kr.co.mindrepublic.coupon.domain.issue.CouponIssueRepository
import kr.co.mindrepublic.coupon.domain.user.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 쿠폰 발급 유스케이스 — "락 없는 순진한 버전".
 *
 * 흐름: (사용자/중복 확인) → 재고 조회 → 잔여 확인·차감 → 발급 이력 저장.
 * 트랜잭션 경계는 이 서비스 메서드(@Transactional)에서 시작·종료한다.
 *
 * !!! Day 3 동시성 경고 !!!
 * 아래 흐름에는 어떠한 동시성 제어(비관/낙관 락, 원자적 UPDATE, 분산 락)도 없다.
 * 여러 트랜잭션이 동시에 같은 stock.remainingQuantity 를 읽고 각자 decrease() 하면
 * lost update 가 발생해, 재고 100개에 2,000명이 몰리면 발급 수가 100을 초과할 수 있다.
 * 이 "깨짐"을 Day 3 동시성 테스트로 재현한 뒤, 이후 단계에서 락으로 고친다.
 * (의도적으로 제어를 넣지 않은 지점이다 — 제거하거나 락을 추가하지 말 것)
 */
@Service
class CouponIssueService(
    private val userRepository: UserRepository,
    private val couponStockRepository: CouponStockRepository,
    private val couponIssueRepository: CouponIssueRepository,
) {

    @Transactional
    fun issue(couponId: Long, userId: Long): CouponIssue {
        // 사용자 존재 확인
        userRepository.findById(userId) ?: throw UserNotFoundException(userId)

        // 1인 1매: 이미 발급받았으면 중복(DB unique 제약과 이중 방어)
        if (couponIssueRepository.existsByCouponIdAndUserId(couponId, userId)) {
            throw DuplicateIssueException(couponId, userId)
        }

        // 재고 조회 (쿠폰당 1행)
        val stock = couponStockRepository.findByCouponId(couponId)
            ?: throw CouponNotFoundException(couponId)

        // <-- Day 3: 여기서 (읽은 재고 값 기준으로) 락 없이 차감한다. 동시 요청 시 lost update.
        stock.decrease()

        // 발급 이력 저장
        return couponIssueRepository.save(CouponIssue(couponId = couponId, userId = userId))
    }
}
