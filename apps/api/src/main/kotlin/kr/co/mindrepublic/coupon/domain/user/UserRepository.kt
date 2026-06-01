package kr.co.mindrepublic.coupon.domain.user

/**
 * 사용자 영속 포트(domain). 구현은 infrastructure 가 제공(의존성 역전).
 */
interface UserRepository {
    fun save(user: User): User
    fun findById(id: Long): User?
    fun findAll(): List<User>
}
