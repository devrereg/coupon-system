package kr.co.mindrepublic.coupon.presentation

import kr.co.mindrepublic.coupon.domain.user.User
import kr.co.mindrepublic.coupon.domain.user.UserRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

/**
 * 사용자 조회 REST API 통합 테스트(MockMvc). 실제 PostgreSQL 사용.
 * 시드 유저 드롭다운용 { id, name } 목록을 검증한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class UserQueryControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val userRepository: UserRepository,
) {

    @Test
    fun `GET users 는 사용자 id 와 name 목록을 반환한다`() {
        val name = "드롭다운 유저-${System.nanoTime()}"
        val userId = userRepository.save(User(name = name)).id!!

        mockMvc.get("/api/users").andExpect {
            status { isOk() }
            jsonPath("$[?(@.id == $userId)].name") { value(name) }
        }
    }
}
