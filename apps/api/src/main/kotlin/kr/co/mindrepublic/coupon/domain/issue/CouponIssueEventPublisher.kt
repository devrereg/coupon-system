package kr.co.mindrepublic.coupon.domain.issue

/**
 * 발급 요청 이벤트 발행 포트(domain). 구현(infrastructure)은 메시지 브로커로 발행한다.
 * 발행 실패(브로커 오류/타임아웃)는 예외로 드러나며, 조율 서비스가 게이트를 보상한다.
 */
interface CouponIssueEventPublisher {
    fun publish(event: CouponIssueRequested)
}
