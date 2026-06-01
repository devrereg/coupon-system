package kr.co.mindrepublic.coupon.domain.coupon

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

/**
 * 쿠폰 정책: 이름, 총 발급 한도(totalQuantity), 발급 기간.
 * 재고(잔여 수량)는 별도 CouponStock 엔티티가 소유한다(쿠폰당 1행).
 */
@Entity
@Table(name = "coupons")
class Coupon(
    @Column(name = "name", nullable = false, length = 200)
    val name: String,

    @Column(name = "total_quantity", nullable = false)
    val totalQuantity: Int,

    @Column(name = "issue_start_at", nullable = false)
    val issueStartAt: LocalDateTime,

    @Column(name = "issue_end_at", nullable = false)
    val issueEndAt: LocalDateTime,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long? = null,
) {
    init {
        require(totalQuantity > 0) { "총 발급 한도는 1 이상이어야 한다: $totalQuantity" }
        require(!issueEndAt.isBefore(issueStartAt)) { "발급 종료 시각은 시작 시각보다 빠를 수 없다" }
    }

    /** 주어진 시각이 발급 가능 기간 안인지. */
    fun isIssuable(at: LocalDateTime): Boolean =
        !at.isBefore(issueStartAt) && !at.isAfter(issueEndAt)
}
