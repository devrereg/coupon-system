package kr.co.mindrepublic.coupon.application

/** 존재하지 않는 쿠폰/재고를 발급 요청한 경우. */
class CouponNotFoundException(couponId: Long) :
    RuntimeException("쿠폰을 찾을 수 없습니다: couponId=$couponId")

/** 존재하지 않는 사용자가 발급 요청한 경우. */
class UserNotFoundException(userId: Long) :
    RuntimeException("사용자를 찾을 수 없습니다: userId=$userId")

/** 이미 발급받은 사용자가 다시 발급 요청한 경우(1인 1매). */
class DuplicateIssueException(couponId: Long, userId: Long) :
    RuntimeException("이미 발급받은 쿠폰입니다: couponId=$couponId, userId=$userId")
