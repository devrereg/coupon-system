package kr.co.mindrepublic.coupon.domain.issue

/**
 * 발급 요청 이벤트 — 게이트를 통과한 "발급 의도".
 * Kafka 토픽으로 발행되어 컨슈머가 DB 영속한다(Day 7 비동기 경로).
 */
data class CouponIssueRequested(
    val couponId: Long,
    val userId: Long,
)
