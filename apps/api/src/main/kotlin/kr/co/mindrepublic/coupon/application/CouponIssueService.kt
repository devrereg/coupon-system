package kr.co.mindrepublic.coupon.application

import kr.co.mindrepublic.coupon.domain.coupon.CouponStockRepository
import kr.co.mindrepublic.coupon.domain.issue.CouponIssue
import kr.co.mindrepublic.coupon.domain.issue.CouponIssueRepository
import kr.co.mindrepublic.coupon.domain.user.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 쿠폰 발급 유스케이스 — 비관 락 버전.
 *
 * 흐름: (사용자/중복 확인) → 재고를 비관 락으로 조회 → 잔여 확인·차감 → 발급 이력 저장.
 * 트랜잭션 경계는 이 서비스 메서드(@Transactional)에서 시작·종료한다.
 *
 * 동시성 제어(Day 4): 재고 조회를 findByCouponIdForUpdate(SELECT … FOR UPDATE)로 수행한다.
 * 같은 재고 row 에 대한 동시 트랜잭션은 쓰기 락을 두고 줄을 서므로 (조회→차감)이 직렬화되어
 * lost update 가 발생하지 않는다. 락은 트랜잭션 커밋 시 해제된다.
 * 단일 row + 일관된 잠금 대상이라 데드락은 없고, 비관 락이라 재시도 로직도 불필요하다.
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

        // 재고 조회 (쿠폰당 1행) — 비관 락으로 동시 차감을 직렬화
        val stock = couponStockRepository.findByCouponIdForUpdate(couponId)
            ?: throw CouponNotFoundException(couponId)

        stock.decrease()

        // 발급 이력 저장
        return couponIssueRepository.save(CouponIssue(couponId = couponId, userId = userId))
    }
}
