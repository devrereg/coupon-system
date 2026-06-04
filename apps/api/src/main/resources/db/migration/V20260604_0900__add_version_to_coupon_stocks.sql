-- Day 5: 낙관 락(@Version)용 버전 컬럼 추가
-- 비관 락과의 비교 실험을 위해 coupon_stocks 에 행 버전을 둔다(append-only, 수정 금지).
ALTER TABLE coupon_stocks ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
