-- KEYS[1] = coupon:stock:{couponId}   (재고 카운터)
-- KEYS[2] = coupon:issued:{couponId}  (발급자 집합)
-- ARGV[1] = userId
-- 재고검사 → 중복검사 → (둘 다 통과 시) SADD + DECR 을 한 스크립트로 원자 실행한다.
-- 반환: -1 소진 / -2 중복 / >=0 통과(남은 재고). 소진을 먼저 봐서 소진 시 발급자 집합을 오염시키지 않는다.
local remaining = tonumber(redis.call('GET', KEYS[1]))
if remaining == nil or remaining <= 0 then
  return -1
end
if redis.call('SISMEMBER', KEYS[2], ARGV[1]) == 1 then
  return -2
end
redis.call('SADD', KEYS[2], ARGV[1])
return redis.call('DECR', KEYS[1])
