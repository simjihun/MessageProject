# 📨 MessageProject — 미니 메시지 발송 시스템

이동통신 메시징 시스템의 축소판을 만들며 다음을 연습하는 프로젝트입니다.

- 비동기 메시지 처리 아키텍처 (API 접수 → 큐 → 워커 발송)
- **멀티쓰레드 데몬 프로그램** (`MessageSendWorker`)
- AWS 배포 (EC2 → RDS → SQS → CI/CD 순으로 확장 예정)

## 아키텍처

```
[웹 화면/REST API] → DB 저장(PENDING) → [MessageQueue(BlockingQueue)]
                                              ↓
                          [워커 쓰레드 x3] 꺼내서 발송 처리 → 상태 갱신(SENT/FAILED)
```

핵심 설계: API는 발송을 직접 하지 않고 큐에 적재 후 즉시 응답합니다.
실제 발송은 백그라운드 워커가 담당합니다. (실무 메시징 시스템과 동일한 패턴)

## 기술 스택

- Java 21, Spring Boot 3.5 (Web, Data JPA, Validation)
- H2 (인메모리 DB) → 추후 AWS RDS로 교체 예정
- `LinkedBlockingQueue` → 추후 AWS SQS로 교체 예정

## 실행 방법

### IntelliJ에서
1. 프로젝트를 열면 Maven이 자동으로 의존성을 내려받음
2. `MessageProjectApplication` 실행

### 터미널에서 (Maven 설치 필요)
```bash
mvn spring-boot:run
```

### 빌드 후 jar 실행 (배포와 동일한 방식)
```bash
mvn clean package
java -jar target/message-project-0.0.1-SNAPSHOT.jar
```

## 확인하기

| 주소 | 설명 |
|---|---|
| http://localhost:8080 | 발송 화면 + 실시간 이력 |
| http://localhost:8080/api/messages | 발송 이력 API |
| http://localhost:8080/api/queue/status | 대기열 상태 |
| http://localhost:8080/healthz | 헬스체크 (AWS LB 연동 대비) |
| http://localhost:8080/h2-console | DB 콘솔 (JDBC URL: `jdbc:h2:mem:messagedb`) |

**멀티쓰레드 동작 확인**: 화면에서 "테스트 10건 한번에 발송"을 누르면
워커 3개(`worker-1~3`)가 동시에 나눠 처리하는 모습을 처리 워커 컬럼과 로그에서 볼 수 있습니다.

## 로드맵

- [x] 1단계: 로컬 동작 (BlockingQueue + 멀티쓰레드 워커 + H2)
- [ ] 2단계: AWS EC2 수동 배포
- [ ] 3단계: H2 → RDS(MySQL), BlockingQueue → SQS 교체
- [ ] 4단계: GitHub Actions 기반 CI/CD 자동 배포
