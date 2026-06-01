import { Button } from "../../atoms/Button";
import { ResultMessage, type ResultVariant } from "../../atoms/ResultMessage";
import { CouponCard, type CouponCardData } from "../../molecules/CouponCard";
import { UserSelect, type UserOption } from "../../molecules/UserSelect";
import styles from "./IssuePanel.module.css";

export interface IssueResultState {
  variant: ResultVariant;
  text: string;
}

export interface IssuePanelProps {
  coupon: CouponCardData | null;
  /** 초기 GET 로딩 중(쿠폰/유저 조회). */
  loading: boolean;
  users: UserOption[];
  selectedUserId: number | null;
  onSelectUser: (userId: number | null) => void;
  /** 발급 POST 진행 중. */
  issuing: boolean;
  onIssue: () => void;
  /** 버튼 비활성(미선택 / 소진 / 빈 상태). */
  issueDisabled: boolean;
  /** 발급 결과. 기본(미발급) 상태면 null → ResultMessage 미렌더. */
  result: IssueResultState | null;
}

/**
 * organism — 발급 패널. CouponCard + UserSelect + Button + ResultMessage 조합.
 * 자체 fetch 없이 상태/핸들러를 page 에서 주입받는다.
 */
export function IssuePanel({
  coupon,
  loading,
  users,
  selectedUserId,
  onSelectUser,
  issuing,
  onIssue,
  issueDisabled,
  result,
}: IssuePanelProps) {
  return (
    <section className={styles.panel}>
      <CouponCard coupon={coupon} loading={loading} />

      <div className={styles.userSelect}>
        <UserSelect
          options={users}
          value={selectedUserId}
          onChange={onSelectUser}
          disabled={loading || issuing}
        />
      </div>

      <div className={styles.action}>
        <Button label="쿠폰 발급" onClick={onIssue} disabled={issueDisabled} loading={issuing} />
      </div>

      {result && (
        <div className={styles.result}>
          <ResultMessage variant={result.variant} text={result.text} />
        </div>
      )}
    </section>
  );
}
