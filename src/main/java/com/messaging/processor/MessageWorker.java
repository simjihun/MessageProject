package com.messaging.processor;

import com.messaging.model.BaseMessage;
import com.messaging.model.MessageStatus;
import com.messaging.queue.MessageQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Consumer: 큐에서 메시지를 꺼내 실제 전송 처리
 * 재전송(Retry) 로직: 실패 시 최대 3회, 지수 백오프(Exponential Backoff) 적용
 */
public abstract class MessageWorker<T extends BaseMessage> implements Runnable {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    private final MessageQueue<T> queue;
    private volatile boolean running = true;

    protected final AtomicLong successCount = new AtomicLong(0);
    protected final AtomicLong failureCount = new AtomicLong(0);

    protected MessageWorker(MessageQueue<T> queue) {
        this.queue = queue;
    }

    @Override
    public void run() {
        log.info("{} started", getClass().getSimpleName());
        while (running) {
            try {
                T message = queue.dequeue();
                if (message != null) {
                    processWithRetry(message);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        log.info("{} stopped. success={} failure={}", getClass().getSimpleName(), successCount.get(), failureCount.get());
    }

    private void processWithRetry(T message) {
        while (true) {
            try {
                boolean success = send(message);
                if (success) {
                    message.updateStatus(MessageStatus.DELIVERED);
                    successCount.incrementAndGet();
                    log.info("DELIVERED: {}", message);
                    return;
                }
            } catch (Exception e) {
                log.warn("Send failed: {} - {}", message, e.getMessage());
            }

            // 재전송 처리 (Store & Forward의 Forward 재시도)
            if (message.canRetry()) {
                message.incrementRetry();
                long backoffMs = (long) Math.pow(2, message.getRetryCount()) * 1000L; // 2s, 4s, 8s
                log.warn("RETRY #{} after {}ms: {}", message.getRetryCount(), backoffMs, message);
                try {
                    Thread.sleep(backoffMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            } else {
                message.updateStatus(MessageStatus.FAILED);
                failureCount.incrementAndGet();
                log.error("FAILED (max retry exceeded): {}", message);
                return;
            }
        }
    }

    /**
     * 실제 전송 로직 - 하위 클래스에서 구현
     * @return true=성공, false=실패(재시도 필요)
     */
    protected abstract boolean send(T message) throws Exception;

    public void stop() { this.running = false; }
    public long getSuccessCount() { return successCount.get(); }
    public long getFailureCount() { return failureCount.get(); }
}
