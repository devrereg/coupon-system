import { Badge, type BadgeTone } from "../../atoms/Badge";
import styles from "./CouponCard.module.css";

export interface CouponCardData {
  name: string;
  totalQuantity: number;
  remaining: number;
}

export interface CouponCardProps {
  /** 쿠폰 데이터. 초기 GET 로딩 중이면 null(쿠폰명 자리 "불러오는 중…"). */
  coupon: CouponCardData | null;
  loading?: boolean;
}

/** 잔여 비율에 따라 Badge 톤/문구를 정한다. 색이 아닌 톤으로만 의미 전달. */
function badgeFor(remaining: number, total: number): { tone: BadgeTone; text: string } {
  if (remaining <= 0) {
    return { tone: "sold-out", text: "품절" };
  }
  // 잔여 임박(≤10%): 검정 폴라리티 플립.
  if (total > 0 && remaining <= total * 0.1) {
    return { tone: "low", text: `잔여 임박 ${remaining}개` };
  }
  return { tone: "normal", text: `잔여 ${remaining}개` };
}

/**
 * molecule — 쿠폰명 + 잔여/총량 + Badge.
 * 데이터를 fetch 하지 않고 props 로만 받는 프레젠테이션.
 */
export function CouponCard({ coupon, loading = false }: CouponCardProps) {
  if (loading || coupon === null) {
    return (
      <div className={styles.card}>
        <p className={styles.muted}>불러오는 중…</p>
      </div>
    );
  }

  const badge = badgeFor(coupon.remaining, coupon.totalQuantity);

  return (
    <div className={styles.card}>
      <h2 className={styles.name}>{coupon.name}</h2>
      <div className={styles.remainingRow}>
        <span className={styles.quantity}>
          <span className={styles.remaining}>{coupon.remaining}</span>
          <span className={styles.total}>/ {coupon.totalQuantity}</span>
        </span>
        <Badge tone={badge.tone} text={badge.text} />
      </div>
    </div>
  );
}
