# Messaging Platform - 이직 포트폴리오

SMS / MMS / App Push 메시지 플랫폼 시뮬레이션 프로젝트.  
이동통신 코어/부가서비스 개발 포지션 학습 목적으로 제작.

## 구현 개념

| 다이어그램 | 이 프로젝트에서 구현한 것 |
|---|---|
| SMSC (SMS Center) | `SmsWorker` - MAP/Diameter 라우팅 시뮬레이션 |
| MMS Relay/Server + MM1~MM8 인터페이스 | `MmsWorker` + `MmsMessage.MmInterface` |
| Push Center → APNS/FCM I/F | `PushWorker` - APNS(TLS) / FCM(HTTPS) 분기 |
| Store & Forward | `MessageQueue<T>` (LinkedBlockingQueue 기반) |
| 재전송 알고리즘 | `MessageWorker.processWithRetry()` - 지수 백오프 |
| CDR 생성 | `CdrRecord` + `QueueMonitor` |
| Admin WEB 모니터링 | `GET /api/messages/stats` |

## 핵심 패턴: Store & Forward

```
[수신] → enqueue() → [BlockingQueue] → dequeue() → [Worker 전송]
  ↑ Store                                              ↓ Forward
  └──────────── 실패 시 지수 백오프 재시도 (최대 3회) ←──┘
```

## 실행 방법

```bash
# 빌드 및 실행
mvn spring-boot:run

# 테스트
mvn test
```

## API 사용 예시

```bash
# SMS 전송 (LTE 네트워크, MT 방향)
curl -X POST http://localhost:8080/api/messages/sms \
  -H "Content-Type: application/json" \
  -d '{"sender":"01011111111","recipient":"01022222222","content":"안녕하세요","network":"LTE","direction":"MT"}'

# MMS 전송
curl -X POST http://localhost:8080/api/messages/mms \
  -H "Content-Type: application/json" \
  -d '{"sender":"01011111111","recipient":"01022222222","subject":"사진","textContent":"첨부 확인해주세요"}'

# iOS Push 전송 (APNS)
curl -X POST http://localhost:8080/api/messages/push \
  -H "Content-Type: application/json" \
  -d '{"recipient":"device-token-abc","platform":"IOS","title":"새 알림","body":"푸시 테스트","badge":1}'

# Android Push 전송 (FCM)
curl -X POST http://localhost:8080/api/messages/push \
  -H "Content-Type: application/json" \
  -d '{"recipient":"fcm-token-xyz","platform":"ANDROID","title":"새 알림","body":"푸시 테스트"}'

# 처리 통계 조회
curl http://localhost:8080/api/messages/stats
```

## 프로젝트 구조

```
src/main/java/com/messaging/
├── model/
│   ├── BaseMessage.java       # 공통 베이스 (messageId, status, retry)
│   ├── SmsMessage.java        # SMS - 3G/LTE/5G, MT/MO 방향
│   ├── MmsMessage.java        # MMS - MM1~MM8 인터페이스, 첨부파일
│   └── PushMessage.java       # Push - APNS(iOS) / FCM(Android) 페이로드
├── queue/
│   ├── MessageQueue.java      # Store & Forward 큐 (BlockingQueue 래핑)
│   └── MessageQueueManager.java # 타입별 큐 관리 (SMS/MMS/Push 분리)
├── processor/
│   ├── MessageWorker.java     # Consumer 추상 클래스 (재전송 로직 포함)
│   ├── SmsWorker.java         # SMSC 전송 시뮬레이션
│   ├── MmsWorker.java         # MMS Relay 전송 시뮬레이션
│   ├── PushWorker.java        # APNS/FCM 전송 시뮬레이션
│   └── WorkerPoolManager.java # 스레드 풀 관리
├── controller/
│   └── MessageController.java # REST API
└── monitor/
    ├── CdrRecord.java         # CDR (과금/통계 레코드)
    └── QueueMonitor.java      # 5초 주기 큐 상태 모니터링
```

## 기술 스택

- **Java 21** (Spring Boot 3.2)
- **`java.util.concurrent`**: `BlockingQueue`, `ExecutorService`, `AtomicLong`
- **Producer-Consumer 패턴**: 대용량 비동기 처리 핵심
- **Exponential Backoff**: 재전송 신뢰성 보장

## 공고 요건 매핑

| 공고 요건 | 구현 내용 |
|---|---|
| Java 서버 개발 | Spring Boot REST API |
| 이동통신 코어/부가서비스 | SMS(SMSC), MMS, Push 플랫폼 |
| 대용량 처리 (수억 건) | BlockingQueue + ThreadPool + 통계 |
| Store & Forward | `MessageQueue<T>` |
| 재전송 알고리즘 | 지수 백오프 (2s, 4s, 8s) |
| CDR 생성 | `CdrRecord` |
