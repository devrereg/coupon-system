# coupon-system / apps/api (백엔드)

선착순 쿠폰 발급 시스템의 백엔드. 모노레포 일부이며, 공용 인프라(`docker compose`)와
툴체인(`.sdkmanrc`)은 **레포 루트**에서 실행한다. 아래 `docker compose` / `sdk env` 명령은
루트(`coupon-system/`)에서, `./gradlew` 명령은 이 디렉토리(`apps/api/`)에서 실행한다.

## 기술 스택 (Day 1)

| 영역 | 선택 |
|---|---|
| Language | Kotlin 1.9.25 |
| Runtime | Java 21 (Temurin, Virtual Threads 활성화) |
| Framework | Spring Boot 3.5.14 |
| Build | Gradle (Kotlin DSL) — 래퍼 8.14.5 |
| DB | PostgreSQL 16 (호스트 포트 15432) |
| Cache/Lock | Redis 7 |
| 툴체인 관리 | SDKMAN (`.sdkmanrc`) |

## 사전 준비

- [SDKMAN](https://sdkman.io) — JDK/Gradle 버전 관리
- Docker / Docker Compose

```bash
# 이 디렉토리에서 .sdkmanrc 기준으로 JDK 21 자동 전환
# (최초 1회) 자동 전환 활성화:
#   sed -i '' 's/sdkman_auto_env=false/sdkman_auto_env=true/' ~/.sdkman/etc/config
sdk env install   # .sdkmanrc 에 명시된 버전 설치
sdk env           # 현재 셸에 적용
```

## 실행 방법

```bash
# 1) 인프라 기동 (PostgreSQL + Redis)
docker compose up -d
docker compose ps          # 두 컨테이너가 healthy 인지 확인

# 2) 빌드 & 테스트  (인프라가 떠 있어야 통과)
./gradlew build

# 3) 애플리케이션 실행
./gradlew bootRun

# 4) 헬스 체크 (db, redis 컴포넌트가 UP 인지 확인)
curl -s http://localhost:8080/actuator/health | jq

# 5) 인프라 종료
docker compose down        # 데이터 유지
docker compose down -v     # 볼륨까지 삭제
```

## Actuator 엔드포인트

| 경로 | 설명 |
|---|---|
| `/actuator/health` | DB·Redis 연결 상태 포함 헬스 체크 |
| `/actuator/info` | 앱·Java 정보 |
| `/actuator/metrics` | 메트릭 목록 |
| `/actuator/prometheus` | Prometheus 포맷 메트릭 (4주차 모니터링에서 사용) |

## 가상 스레드(Virtual Threads) 확인

`application.yml`의 `spring.threads.virtual.enabled=true` 로 톰캣 요청이 가상 스레드에서 처리된다.
`VirtualThreadSmokeTest`가 실제 HTTP 왕복으로 이를 검증한다.

## 학습 로드맵

- **1주차** 동시성 제어 + Spring AOP (분산 락)
- **2주차** Kafka / Event-Driven Architecture
- **3주차** Spring Batch (대용량 정산)
- **4주차** Kubernetes + Prometheus/Grafana 모니터링
