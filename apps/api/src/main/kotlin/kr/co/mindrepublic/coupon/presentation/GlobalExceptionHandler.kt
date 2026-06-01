package kr.co.mindrepublic.coupon.presentation

import kr.co.mindrepublic.coupon.application.CouponNotFoundException
import kr.co.mindrepublic.coupon.application.DuplicateIssueException
import kr.co.mindrepublic.coupon.application.UserNotFoundException
import kr.co.mindrepublic.coupon.domain.coupon.OutOfStockException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.LocalDateTime

/**
 * 도메인/유스케이스 예외를 HTTP 상태로 매핑한다. 컨트롤러는 예외 처리를 신경 쓰지 않는다.
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    data class ErrorResponse(
        val status: Int,
        val error: String,
        val message: String,
        val timestamp: LocalDateTime = LocalDateTime.now(),
    )

    // 재고 소진 / 중복 발급 → 409 Conflict (요청 자체는 유효하나 현재 상태와 충돌)
    @ExceptionHandler(OutOfStockException::class)
    fun handleOutOfStock(e: OutOfStockException): ResponseEntity<ErrorResponse> =
        build(HttpStatus.CONFLICT, e.message ?: "재고가 소진되었습니다")

    @ExceptionHandler(DuplicateIssueException::class)
    fun handleDuplicate(e: DuplicateIssueException): ResponseEntity<ErrorResponse> =
        build(HttpStatus.CONFLICT, e.message ?: "이미 발급받았습니다")

    // 쿠폰/사용자 없음 → 404 Not Found
    @ExceptionHandler(CouponNotFoundException::class, UserNotFoundException::class)
    fun handleNotFound(e: RuntimeException): ResponseEntity<ErrorResponse> =
        build(HttpStatus.NOT_FOUND, e.message ?: "리소스를 찾을 수 없습니다")

    // 요청 검증 실패 → 400 Bad Request
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(e: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val message = e.bindingResult.fieldErrors
            .joinToString(", ") { "${it.field}: ${it.defaultMessage}" }
            .ifBlank { "요청이 올바르지 않습니다" }
        return build(HttpStatus.BAD_REQUEST, message)
    }

    private fun build(status: HttpStatus, message: String): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(status).body(
            ErrorResponse(status = status.value(), error = status.reasonPhrase, message = message),
        )
}
