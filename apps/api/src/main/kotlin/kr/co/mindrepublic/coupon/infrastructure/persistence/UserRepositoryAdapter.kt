package kr.co.mindrepublic.coupon.infrastructure.persistence

import kr.co.mindrepublic.coupon.domain.user.User
import kr.co.mindrepublic.coupon.domain.user.UserRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

/**
 * domain UserRepository 포트를 Spring Data JPA 로 구현하는 어댑터(의존성 역전).
 */
@Repository
class UserRepositoryAdapter(
    private val jpa: UserJpaRepository,
) : UserRepository {
    override fun save(user: User): User = jpa.save(user)
    override fun findById(id: Long): User? = jpa.findByIdOrNull(id)
    override fun findAll(): List<User> = jpa.findAll()
}
