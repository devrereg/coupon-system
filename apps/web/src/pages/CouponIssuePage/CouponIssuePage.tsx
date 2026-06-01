import { useEffect, useState } from "react";
import {
  ApiError,
  getCoupons,
  getUsers,
  issueCoupon,
  type Coupon,
  type User,
} from "../../api";
import { IssuePanel, type IssueResultState } from "../../components/organisms/IssuePanel";
import styles from "./CouponIssuePage.module.css";

/** issuedAt(ISO) 을 사람이 읽는 시각으로. 실패하면 원본을 그대로 보여준다. */
function formatIssuedAt(iso: string): string {
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) {
    return iso;
  }
  return date.toLocaleString("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
  });
}

/**
 * page — 데이터 fetch(getCoupons/getUsers/issueCoupon) 와 6개 상태를 관리하고
 * IssuePanel 에 주입한다. fetch 책임은 이 컴포넌트에만 있다.
 */
export function CouponIssuePage() {
  const [coupon, setCoupon] = useState<Coupon | null>(null);
  const [users, setUsers] = useState<User[]>([]);
  const [selectedUserId, setSelectedUserId] = useState<number | null>(null);

  // 초기 GET(쿠폰+유저) 로딩.
  const [loading, setLoading] = useState(true);
  // 발급 POST 진행.
  const [issuing, setIssuing] = useState(false);
  // 발급 결과(미발급이면 null).
  const [result, setResult] = useState<IssueResultState | null>(null);

  // 마운트: 쿠폰(첫 항목=활성) + 유저 동시 조회.
  useEffect(() => {
    // loading 은 초기값이 true 이므로 effect 안에서 동기로 다시 set 하지 않는다.
    let cancelled = false;
    Promise.all([getCoupons(), getUsers()])
      .then(([coupons, fetchedUsers]) => {
        if (cancelled) return;
        setCoupon(coupons[0] ?? null);
        setUsers(fetchedUsers);
      })
      .catch(() => {
        if (cancelled) return;
        setCoupon(null);
        setUsers([]);
        setResult({ variant: "error", text: "데이터를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요." });
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const soldOut = coupon !== null && coupon.remaining <= 0;
  const isEmpty = !loading && (coupon === null || users.length === 0);

  // 버튼 비활성: 로딩 / 발급중 / 유저 미선택 / 소진 / 빈 상태.
  const issueDisabled =
    loading || issuing || selectedUserId === null || soldOut || isEmpty;

  async function handleIssue() {
    if (coupon === null || selectedUserId === null) return;
    setIssuing(true);
    try {
      const issued = await issueCoupon(coupon.id, selectedUserId);
      // 성공: 잔여 재조회로 숫자 갱신(감소).
      const refreshed = await getCoupons();
      setCoupon(refreshed.find((c) => c.id === coupon.id) ?? refreshed[0] ?? coupon);
      setResult({
        variant: "success",
        text: `발급 완료 · ${formatIssuedAt(issued.issuedAt)}`,
      });
    } catch (err) {
      handleIssueError(err);
    } finally {
      setIssuing(false);
    }
  }

  /** 발급 에러를 4/5/6 상태로 매핑. 409 는 message 로 소진/중복 구분. */
  function handleIssueError(err: unknown) {
    if (err instanceof ApiError) {
      if (err.status === 409) {
        if (err.message.includes("이미 발급")) {
          // 중복발급: 버튼 활성 유지(다른 유저로 재시도).
          setResult({
            variant: "duplicate",
            text: "이미 발급받은 유저입니다. 다른 유저를 선택해 주세요.",
          });
          return;
        }
        // 재고 소진: 잔여를 0 으로 반영 → 소진 상태 진입.
        if (coupon !== null) {
          setCoupon({ ...coupon, remaining: 0 });
        }
        setResult({ variant: "soldout", text: "재고가 모두 소진되어 발급할 수 없습니다." });
        return;
      }
      // 400/404 등 일반 에러: 버튼 활성.
      setResult({ variant: "error", text: err.message });
      return;
    }
    // 네트워크 등: 버튼 활성.
    setResult({
      variant: "error",
      text: "발급 요청에 실패했습니다. 잠시 후 다시 시도해 주세요.",
    });
  }

  // 빈 상태(쿠폰/유저 없음): 결과 메시지가 따로 없을 때만 안내 노출.
  const panelResult =
    isEmpty && result === null
      ? ({ variant: "error", text: "표시할 쿠폰이 없습니다." } as IssueResultState)
      : result;

  return (
    <main className={styles.page}>
      <div className={styles.container} style={{ maxWidth: 480 }}>
        <header className={styles.header}>
          <h1 className={styles.title}>쿠폰 발급</h1>
          <p className={styles.subtitle}>유저를 선택하고 선착순 쿠폰을 발급받으세요.</p>
        </header>

        <IssuePanel
          coupon={coupon}
          loading={loading}
          users={users}
          selectedUserId={selectedUserId}
          onSelectUser={setSelectedUserId}
          issuing={issuing}
          onIssue={handleIssue}
          issueDisabled={issueDisabled}
          result={panelResult}
        />
      </div>
    </main>
  );
}
