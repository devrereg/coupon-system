package kr.co.mindrepublic.coupon.infrastructure.persistence

import kr.co.mindrepublic.coupon.domain.user.User
import org.springframework.data.jpa.repository.JpaRepository

/**
 * Spring Data JPA 가 구현을 생성하는 내부 인터페이스. domain 은 이 존재를 모른다.
 */
interface UserJpaRepository : JpaRepository<User, Long>
