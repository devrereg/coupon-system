package kr.co.mindrepublic.coupon.domain.coupon

/** 1인 1매 선차단 게이트(reserveWithDedup)의 3-state 결과. */
enum class ReservationResult { RESERVED, OUT_OF_STOCK, DUPLICATE }
