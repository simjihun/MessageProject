# 📨 MessageProject — 메시지 게이트웨이 관제 콘솔

이동통신 메시징 시스템의 축소판을 구현하고 AWS에 배포·운영하는 프로젝트입니다.

## 주요 기능

- **비동기 발송 파이프라인**: API 접수 → 큐 적재 → 멀티쓰레드 워커 발송 → 상태 갱신
- **멀티쓰레드 발송 데몬**: 워커 쓰레드 N개가 큐를 공유하며 병렬 처리, Graceful Shutdown 지원
- **자동 재시도**: 발송 실패 시 최대 2회 자동 재큐잉 후 최종 실패 확정
- **수동 재발송**: FAILED 건 클릭 한 번으로 재발송
- **관제 대시보드**: 실시간 통계 · 워커별 처리 현황(라이브 파이프라인) · 분당 처리량 차트(최근 30분)
- **발송 이력 검색**: 상태 필터 + 수신번호 검색 + 페이징
- **일자별 로그 롤링**: 당일 로그는 `app.log`, 자정이 지나면 `YYYY-MM-DD.log`로 자동 보관 (14일 보존)

## 아키텍처

```
[관제 콘솔 / REST API]
        │ 접수 즉시 응답 (비동기)
        ▼
   DB 저장(PENDING) ──▶ [MessageQueue · BlockingQueue]
                              │
              ┌─────────────┼────────────┐
         [worker-1]      [worker-2]     [worker-3]
              └── 발송 시뮬레이션 → 성공(SENT) / 재시도 재큐잉 / 최종실패(FAILED)
```

## 기술 스택

- Java 21, Spring Boot 3.5 (Web, Data JPA, Validation), Logback
- DB: H2(로컬 기본) / MySQL on AWS RDS(운영 `prod` 프로파일)
- `LinkedBlockingQueue` → 추후 AWS SQS로 교체 예정
- Vanilla JS + Chart.js (관제 대시보드)
- 인프라: AWS EC2 (Amazon Linux 2023, t3.micro) + RDS

## 운영 배치 구조 (EC2)

```
/home/jihun/
├─ Start.sh          # 백그라운드 실행 (중복 실행 방지, 설정 주입)
├─ killall.sh        # Graceful Shutdown (10초 대기 후 강제 종료)
├─ conf/app.conf     # 환경 설정 (포트, DB 접속 정보) — 코드와 설정 분리
├─ libs/message-project-0.0.1-SNAPSHOT.jar
└─ logs/
   ├─ app.log        # 당일 로그 (Logback 일자별 롤링)
   ├─ 2026-07-30.log # 지난 날짜 보관본
   └─ console.log    # JVM 기동 실패 대비 출력
```

설정값은 코드가 아닌 `conf/app.conf`에서 환경변수로 주입받으며(환경 독립적 설계),
`SPRING_PROFILES_ACTIVE=prod` 설정 시 RDS(MySQL)로, 미설정 시 H2로 동작합니다.

## 실행 방법

```bash
# 로컬 개발 (H2 인메모리, 포트 8081)
mvn spring-boot:run

# 빌드
mvn clean package

# EC2 운영
./Start.sh      # 시작
./killall.sh    # 종료
```

## 주소

| 주소 | 설명 |
|---|---|
| `/` | 관제 콘솔 (대시보드 · 발송 · 이력) |
| `/api/messages` | 발송 접수(POST) · 이력 검색(GET, 페이징) |
| `/api/messages/{id}/resend` | 실패 건 재발송 |
| `/api/stats` | 대시보드 통계 (상태별 집계 · 워커 상태 · 분당 처리량) |
| `/healthz` | 헬스체크 (AWS LB 연동 대비) |

## 로드맵

- [x] 1단계: 로컬 동작 (BlockingQueue + 멀티쓰레드 워커 + H2)
- [x] 2단계: AWS EC2 수동 배포 + 운영 스크립트
- [x] 2.5단계: 관제 대시보드 · 자동 재시도 · 재발송 · 이력 검색
- [x] 2.7단계: 운영 디렉토리 구조화 (conf/libs/logs 분리) + 일자별 로그 롤링
- [ ] 3단계: H2 → AWS RDS(MySQL) 전환 — 진행 중
- [ ] 4단계: BlockingQueue → AWS SQS 교체
- [ ] 5단계: GitHub Actions 기반 CI/CD 자동 배포
