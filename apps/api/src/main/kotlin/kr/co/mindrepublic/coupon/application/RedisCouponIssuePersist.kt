package kr.co.mindrepublic.coupon.application

import kr.co.mindrepublic.coupon.domain.issue.CouponIssue
import kr.co.mindrepublic.coupon.domain.issue.CouponIssueRepository
import kr.co.mindrepublic.coupon.domain.user.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Redis 게이트 발급 경로의 "DB 영속 한 번" — 사용자/중복 확인 후 발급 이력을 저장한다.
 *
 * 재고(coupon_stocks)는 건드리지 않는다: 재고 권위는 Redis 카운터에 있다(Day 6).
 * 조율 서비스(RedisCouponIssueService)와 **별도 빈**으로 둔 이유 —
 * (1) @Transactional 이 프록시로 정상 적용되도록(자기호출 함정 회피, Day 5와 동일),
 * (2) 보상(release)은 이 트랜잭션이 커밋/롤백된 뒤 그 바깥에서 결정해야 하기 때문.
 */
@Service
class RedisCouponIssuePersist(
    private val userRepository: UserRepository,
    private val couponIssueRepository: CouponIssueRepository,
) {

    @Transactional
    fun persist(couponId: Long, userId: Long): CouponIssue {
        userRepository.findById(userId) ?: throw UserNotFoundException(userId)

        if (couponIssueRepository.existsByCouponIdAndUserId(couponId, userId)) {
            throw DuplicateIssueException(couponId, userId)
        }

        return couponIssueRepository.save(CouponIssue(couponId = couponId, userId = userId))
    }
}
