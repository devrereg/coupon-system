package kr.co.mindrepublic.coupon.application

import kr.co.mindrepublic.coupon.domain.coupon.StockReservation
import kr.co.mindrepublic.coupon.domain.issue.CouponIssueRequested
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

/**
 * 발급 요청 이벤트 컨슈머 — Redis 게이트를 통과한 의도를 DB 에 영속한다(Day 7 비동기 경로).
 * 영속은 기존 RedisCouponIssuePersist 를 재사용한다(재고 미터치, 사용자/중복 확인 + 발급 이력 저장).
 *
 * 보상 분기(at-least-once 멱등성 핵심):
 *  - DuplicateIssueException(DB unique 위반): SET 선차단이 있으므로 이 위반은 **Kafka 재배달**(같은 메시지 2회 처리)일 때만
 *    발생한다. 그 행은 이미 정당하게 슬롯을 보유 → **반납하면 초과발급**이므로 반납 없이 ack 로 폐기.
 *  - 그 외 영구 실패(예: UserNotFound): 깎인 슬롯이 헛됨 → releaseWithDedup 로 반납 후 폐기.
 *    (전이성 오류와 영구 오류를 구분하지 않는 단순화 — 재시도/DLQ 는 범위 밖이라 모두 폐기.)
 */
@Component
class CouponIssueEventConsumer(
    private val persist: RedisCouponIssuePersist,
    private val reservation: StockReservation,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(topics = ["coupon-issue-requests"], groupId = "coupon-issue-consumer")
    fun consume(event: CouponIssueRequested, ack: Acknowledgment) {
        try {
            persist.persist(event.couponId, event.userId)
        } catch (e: DuplicateIssueException) {
            log.warn("멱등 중복(재배달) 폐기 — couponId={} userId={}", event.couponId, event.userId)
        } catch (e: Exception) {
            log.warn(
                "영속 실패 → 슬롯 반납 — couponId={} userId={}",
                event.couponId, event.userId, e,
            )
            reservation.releaseWithDedup(event.couponId, event.userId)
        } finally {
            ack.acknowledge()
        }
    }
}
