package kr.co.mindrepublic.coupon.presentation.dto

import kr.co.mindrepublic.coupon.application.UserSummary

/**
 * 사용자 조회 응답. 시드 유저 드롭다운용 최소 필드.
 */
data class UserResponse(
    val id: Long,
    val name: String,
) {
    companion object {
        fun from(summary: UserSummary): UserResponse =
            UserResponse(id = summary.id, name = summary.name)
    }
}
