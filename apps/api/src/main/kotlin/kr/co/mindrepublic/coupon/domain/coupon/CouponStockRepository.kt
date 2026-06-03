package kr.co.mindrepublic.coupon.domain.coupon

/**
 * 쿠폰 재고 영속 포트(domain). 구현은 infrastructure 가 제공.
 */
interface CouponStockRepository {
    fun save(stock: CouponStock): CouponStock
    fun findByCouponId(couponId: Long): CouponStock?

    /**
     * 재고 row 를 비관적 쓰기 락(SELECT … FOR UPDATE)으로 조회한다.
     * 동시 발급 시 (조회→차감→커밋)을 row 단위로 직렬화해 lost update 를 막는다.
     * 락은 호출한 트랜잭션이 끝날 때(커밋/롤백) 해제된다.
     */
    fun findByCouponIdForUpdate(couponId: Long): CouponStock?
}
