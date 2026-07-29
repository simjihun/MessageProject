package com.jihun.message.worker;

import com.jihun.message.domain.Message;
import com.jihun.message.queue.MessageQueue;
import com.jihun.message.repository.MessageRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 멀티쓰레드 발송 워커 (데몬).
 *
 * 앱이 시작되면 워커 쓰레드 N개가 백그라운드에서 무한 루프를 돌며
 * 큐에서 메시지를 꺼내 발송 처리한다.
 * 채용공고의 "멀티쓰레드 환경의 데몬 프로그램"에 해당하는 구조.
 *
 * 실제 시스템이라면 여기서 SMPP 프로토콜로 통신사 서버와 소켓 통신을 하겠지만,
 * 이 프로젝트에서는 Thread.sleep()으로 발송 소요 시간을 시뮬레이션한다.
 */
@Component
public class MessageSendWorker {

    private static final Logger log = LoggerFactory.getLogger(MessageSendWorker.class);

    private final MessageQueue messageQueue;
    private final MessageRepository messageRepository;

    @Value("${app.worker.count}")
    private int workerCount;

    @Value("${app.worker.send-delay-ms}")
    private long sendDelayMs;

    private ExecutorService executor;

    // volatile: 여러 쓰레드가 이 값을 읽을 때 항상 최신 값을 보도록 보장
    private volatile boolean running = true;

    public MessageSendWorker(MessageQueue messageQueue, MessageRepository messageRepository) {
        this.messageQueue = messageQueue;
        this.messageRepository = messageRepository;
    }

    /** 앱 시작 시 워커 쓰레드들을 띄운다 */
    @PostConstruct
    public void start() {
        executor = Executors.newFixedThreadPool(workerCount);
        for (int i = 1; i <= workerCount; i++) {
            String workerName = "worker-" + i;
            executor.submit(() -> runLoop(workerName));
        }
        log.info("메시지 발송 워커 {}개 시작", workerCount);
    }

    /** 각 워커 쓰레드가 실행하는 무한 루프 */
    private void runLoop(String workerName) {
        Thread.currentThread().setName(workerName);
        log.info("[{}] 발송 루프 시작", workerName);

        while (running) {
            try {
                // 큐가 비어 있으면 1초 대기 후 null → running 플래그를 다시 확인
                Long messageId = messageQueue.poll(1, TimeUnit.SECONDS);
                if (messageId == null) {
                    continue;
                }
                process(workerName, messageId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                // 한 건 실패해도 워커는 죽지 않고 다음 메시지를 계속 처리해야 한다
                log.error("[{}] 처리 중 예외 발생", workerName, e);
            }
        }
        log.info("[{}] 발송 루프 종료", workerName);
    }

    /** 메시지 1건 발송 처리 */
    private void process(String workerName, Long messageId) throws InterruptedException {
        Message message = messageRepository.findById(messageId).orElse(null);
        if (message == null) {
            log.warn("[{}] 메시지를 찾을 수 없음 id={}", workerName, messageId);
            return;
        }

        message.markSending(workerName);
        messageRepository.save(message);
        log.info("[{}] 발송 시작 id={} to={}", workerName, messageId, message.getReceiver());

        // === 발송 시뮬레이션 (실제라면 SMPP 소켓 통신 구간) ===
        Thread.sleep(sendDelayMs);

        // 90% 성공, 10% 실패로 시뮬레이션
        boolean success = ThreadLocalRandom.current().nextInt(100) < 90;
        if (success) {
            message.markSent();
            log.info("[{}] 발송 성공 id={}", workerName, messageId);
        } else {
            message.markFailed();
            log.warn("[{}] 발송 실패 id={}", workerName, messageId);
        }
        messageRepository.save(message);
    }

    /** 앱 종료 시 워커들을 정리한다 (Graceful Shutdown) */
    @PreDestroy
    public void stop() throws InterruptedException {
        log.info("워커 종료 신호 전송");
        running = false;
        executor.shutdown();
        if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
            executor.shutdownNow();
        }
        log.info("워커 전체 종료 완료");
    }
}
