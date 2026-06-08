package kr.co.mindrepublic.coupon.infrastructure.messaging

import kr.co.mindrepublic.coupon.domain.issue.CouponIssueEventPublisher
import kr.co.mindrepublic.coupon.domain.issue.CouponIssueRequested
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

/**
 * CouponIssueEventPublisher 의 Kafka 구현.
 * 키 = couponId → 같은 쿠폰 메시지는 같은 파티션(순서 보장).
 * send().get() 으로 acks=all 확인을 **동기**로 기다려, 발행 실패를 호출자가 즉시 알게 한다.
 */
@Component
class KafkaCouponIssueEventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, CouponIssueRequested>,
) : CouponIssueEventPublisher {

    override fun publish(event: CouponIssueRequested) {
        kafkaTemplate.send(TOPIC, event.couponId.toString(), event)
            .get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS)
    }

    companion object {
        const val TOPIC = "coupon-issue-requests"
        private const val SEND_TIMEOUT_SECONDS = 10L
    }
}
