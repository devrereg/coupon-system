package kr.co.mindrepublic.coupon.presentation

import kr.co.mindrepublic.coupon.application.UserQueryService
import kr.co.mindrepublic.coupon.presentation.dto.UserResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 사용자 조회 API. 시드 유저 드롭다운용 목록을 응답 DTO 로 내려준다.
 */
@RestController
@RequestMapping("/api/users")
class UserController(
    private val userQueryService: UserQueryService,
) {

    @GetMapping
    fun list(): List<UserResponse> =
        userQueryService.findAll().map(UserResponse::from)
}
