package kr.co.mindrepublic.coupon.domain.user

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

/**
 * 발급 주체. CouponIssue 가 FK 로 참조하는 최소 사용자 엔티티.
 */
@Entity
@Table(name = "users")
class User(
    @Column(name = "name", nullable = false, length = 100)
    val name: String,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long? = null,
)
