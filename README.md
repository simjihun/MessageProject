# 📨 MessageProject — 메시지 게이트웨이 관제 콘솔

이동통신 메시징 시스템의 축소판을 구현한 프로젝트입니다.

## 주요 기능

- **비동기 발송 파이프라인**: API 접수 → 큐 적재 → 멀티쓰레드 워커 발송 → 상태 갱신
- **멀티쓰레드 발송 데몬**: 워커 쓰레드 N개가 큐를 공유하며 병렬 처리, Graceful Shutdown 지원
- **자동 재시도**: 발송 실패 시 최대 2회 자동 재큐잉 후 최종 실패 확정
- **수동 재발송**: FAILED 건 클릭 한 번으로 재발송
- **관제 대시보드**: 실시간 통계 · 워커별 처리 현황(라이브 파이프라인) · 분당 처리량 차트(최근 30분)
- **발송 이력 검색**: 상태 필터 + 수신번호 검색 + 페이징

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

API는 발송을 직접 하지 않고 큐에 적재 후 즉시 응답합니다. 실제 발송은 백그라운드 워커가 담당합니다.

## 기술 스택

- Java 21, Spring Boot 3.5 (Web, Data JPA, Validation)
- H2 (인메모리 DB) → 추후 AWS RDS로 교체 예정
- `LinkedBlockingQueue` → 추후 AWS SQS로 교체 예정
- Vanilla JS + Chart.js (관제 대시보드)

## 실행 방법

```bash
# 개발 실행
mvn spring-boot:run

# 빌드 후 jar 실행 (배포와 동일)
mvn clean package
java -jar target/message-project-0.0.1-SNAPSHOT.jar
```

기본 포트는 8081이며 환경변수로 변경 가능합니다: `SERVER_PORT=8080 java -jar app.jar`

### EC2 운영 스크립트

```bash
./scripts/Start.sh     # 백그라운드 실행 (nohup, 중복 실행 방지)
./scripts/killall.sh   # Graceful Shutdown (10초 대기 후 강제 종료)
```

## 주소

| 주소 | 설명 |
|---|---|
| `/` | 관제 콘솔 (대시보드 · 발송 · 이력) |
| `/api/messages` | 발송 접수(POST) · 이력 검색(GET, 페이징) |
| `/api/messages/{id}/resend` | 실패 건 재발송 |
| `/api/stats` | 대시보드 통계 (상태별 집계 · 워커 상태 · 분당 처리량) |
| `/healthz` | 헬스체크 (AWS LB 연동 대비) |
| `/h2-console` | DB 콘솔 (JDBC URL: `jdbc:h2:mem:messagedb`) |

## 로드맵

- [x] 1단계: 로컬 동작 (BlockingQueue + 멀티쓰레드 워커 + H2)
- [x] 2단계: AWS EC2 수동 배포 + 운영 스크립트
- [x] 2.5단계: 관제 대시보드 · 자동 재시도 · 재발송 · 이력 검색
- [ ] 3단계: H2 → RDS(MySQL), BlockingQueue → SQS 교체
- [ ] 4단계: GitHub Actions 기반 CI/CD 자동 배포
