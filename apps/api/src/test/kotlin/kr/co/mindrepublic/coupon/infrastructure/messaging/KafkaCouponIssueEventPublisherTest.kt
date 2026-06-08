package kr.co.mindrepublic.coupon.infrastructure.messaging

import kr.co.mindrepublic.coupon.domain.issue.CouponIssueEventPublisher
import kr.co.mindrepublic.coupon.domain.issue.CouponIssueRequested
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit

/**
 * 발행 어댑터가 실제 브로커로 메시지를 보내고, 컨슈머가 같은 페이로드로 역직렬화해 받는지 검증.
 * NOTE: Kafka 가 떠 있어야 한다(docker compose up -d).
 */
@SpringBootTest
class KafkaCouponIssueEventPublisherTest @Autowired constructor(
    private val publisher: CouponIssueEventPublisher,
    private val recorder: TestRecorder,
) {
    @Test
    fun `발행한 이벤트를 컨슈머가 같은 페이로드로 받는다`() {
        val event = CouponIssueRequested(couponId = 777_001L, userId = 42L)

        publisher.publish(event)

        await().atMost(10, TimeUnit.SECONDS).untilAsserted {
            assertThat(recorder.received).contains(event)
        }
    }

    @TestConfiguration
    class RecorderConfig {
        @Bean
        fun testRecorder() = TestRecorder()
    }

    class TestRecorder {
        val received = ConcurrentLinkedQueue<CouponIssueRequested>()

        @KafkaListener(topics = ["coupon-issue-requests"], groupId = "publisher-test-recorder")
        fun listen(event: CouponIssueRequested) {
            received.add(event)
        }
    }
}
