package kr.co.mindrepublic.coupon.infrastructure.messaging

import org.apache.kafka.clients.admin.NewTopic
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.TopicBuilder

/** 발급 요청 토픽 — 부팅 시 자동 생성. 파티션 3(학습용 소수), 복제 1(단일 브로커). */
@Configuration
class KafkaTopicConfig {
    @Bean
    fun couponIssueRequestsTopic(): NewTopic =
        TopicBuilder.name(KafkaCouponIssueEventPublisher.TOPIC)
            .partitions(3)
            .replicas(1)
            .build()
}
