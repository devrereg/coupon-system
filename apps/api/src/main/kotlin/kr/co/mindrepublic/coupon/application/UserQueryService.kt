package kr.co.mindrepublic.coupon.application

import kr.co.mindrepublic.coupon.domain.user.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 사용자 조회 유스케이스(읽기 전용). 시드 유저 드롭다운에 쓰일 { id, name } 목록을 내려준다.
 */
@Service
class UserQueryService(
    private val userRepository: UserRepository,
) {

    @Transactional(readOnly = true)
    fun findAll(): List<UserSummary> =
        userRepository.findAll().map { user ->
            UserSummary(
                id = requireNotNull(user.id) { "사용자 id 가 없습니다" },
                name = user.name,
            )
        }
}

/**
 * 사용자 조회 결과(application DTO).
 */
data class UserSummary(
    val id: Long,
    val name: String,
)
