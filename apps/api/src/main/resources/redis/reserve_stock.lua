-- KEYS[1] = coupon:stock:{couponId}
-- 잔여 > 0 이면 DECR 후 남은 값(>=0)을 반환, 잔여가 없으면(또는 키 없음) 차감하지 않고 -1 반환.
-- GET 검사와 DECR 을 한 스크립트에서 원자적으로 실행해 naive DECR 의 음수 창(window)을 없앤다.
local remaining = tonumber(redis.call('GET', KEYS[1]))
if remaining == nil or remaining <= 0 then
  return -1
end
return redis.call('DECR', KEYS[1])
