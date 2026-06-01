package kr.co.mindrepublic.coupon.application

import kr.co.mindrepublic.coupon.domain.coupon.CouponRepository
import kr.co.mindrepublic.coupon.domain.coupon.CouponStockRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 쿠폰 조회 유스케이스(읽기 전용). 쿠폰 정책(Coupon)과 재고(CouponStock)를 조합해
 * 잔여 수량을 포함한 application 결과로 내려준다. presentation DTO 는 알지 않는다.
 */
@Service
class CouponQueryService(
    private val couponRepository: CouponRepository,
    private val couponStockRepository: CouponStockRepository,
) {

    /** 쿠폰 목록 + 각 쿠폰의 잔여 수량. 재고 행이 없으면 잔여 0 으로 본다. */
    @Transactional(readOnly = true)
    fun findAll(): List<CouponSummary> =
        couponRepository.findAll().map { coupon ->
            val couponId = requireNotNull(coupon.id) { "쿠폰 id 가 없습니다" }
            val remaining = couponStockRepository.findByCouponId(couponId)?.remainingQuantity ?: 0
            CouponSummary(
                id = couponId,
                name = coupon.name,
                totalQuantity = coupon.totalQuantity,
                remaining = remaining,
            )
        }
}

/**
 * 쿠폰 조회 결과(application DTO). 엔티티를 레이어 밖으로 흘리지 않기 위한 평탄화 모델.
 */
data class CouponSummary(
    val id: Long,
    val name: String,
    val totalQuantity: Int,
    val remaining: Int,
)
