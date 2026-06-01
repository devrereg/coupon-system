package kr.co.mindrepublic.coupon.domain.issue

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

/**
 * 발급 이력: 누가(userId) 어떤 쿠폰(couponId)을 언제(issuedAt) 발급했는지.
 *
 * (coupon_id, user_id) unique 제약으로 1인 1매를 DB 레벨에서도 보장한다(Q3).
 * coupon/user 는 FK id 만 보관해 발급 경로를 가볍게 유지한다(연관 객체 그래프 로딩 회피).
 */
@Entity
@Table(
    name = "coupon_issues",
    uniqueConstraints = [
        UniqueConstraint(name = "uq_coupon_issues_coupon_user", columnNames = ["coupon_id", "user_id"]),
    ],
)
class CouponIssue(
    @Column(name = "coupon_id", nullable = false)
    val couponId: Long,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "issued_at", nullable = false)
    val issuedAt: LocalDateTime = LocalDateTime.now(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long? = null,
)
