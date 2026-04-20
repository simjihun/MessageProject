package com.messaging.processor;

import com.messaging.queue.MessageQueueManager;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Worker 스레드 풀 관리
 * 대용량 처리를 위해 각 큐마다 여러 Worker를 병렬 실행
 *
 * 실제 시스템: 수억 건 처리를 위해 수십~수백 Worker 운영
 * 이 데모: 각 타입별 Worker 수를 설정으로 조절 가능
 */
@Component
public class WorkerPoolManager {

    private static final Logger log = LoggerFactory.getLogger(WorkerPoolManager.class);

    private static final int SMS_WORKERS  = 3;
    private static final int MMS_WORKERS  = 2;
    private static final int PUSH_WORKERS = 5;

    private final MessageQueueManager queueManager;
    private final ExecutorService executor;
    private final List<MessageWorker<?>> workers = new ArrayList<>();

    public WorkerPoolManager(MessageQueueManager queueManager) {
        this.queueManager = queueManager;
        int totalWorkers = SMS_WORKERS + MMS_WORKERS + PUSH_WORKERS;
        this.executor = Executors.newFixedThreadPool(totalWorkers,
                r -> {
                    Thread t = new Thread(r);
                    t.setDaemon(true);
                    return t;
                });
    }

    @PostConstruct
    public void start() {
        // SMS Workers
        for (int i = 0; i < SMS_WORKERS; i++) {
            SmsWorker worker = new SmsWorker(queueManager.getSmsQueue());
            workers.add(worker);
            executor.submit(worker);
        }

        // MMS Workers
        for (int i = 0; i < MMS_WORKERS; i++) {
            MmsWorker worker = new MmsWorker(queueManager.getMmsQueue());
            workers.add(worker);
            executor.submit(worker);
        }

        // Push Workers (가장 많이 필요 - 대량 발송)
        for (int i = 0; i < PUSH_WORKERS; i++) {
            PushWorker worker = new PushWorker(queueManager.getPushQueue());
            workers.add(worker);
            executor.submit(worker);
        }

        log.info("Worker pool started: SMS={}, MMS={}, Push={}", SMS_WORKERS, MMS_WORKERS, PUSH_WORKERS);
    }

    @PreDestroy
    public void stop() {
        workers.forEach(MessageWorker::stop);
        executor.shutdown();
        try {
            executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log.info("Worker pool stopped");
    }

    public WorkerStats getStats() {
        long smsSuccess = workers.stream()
                .filter(w -> w instanceof SmsWorker)
                .mapToLong(MessageWorker::getSuccessCount).sum();
        long smsFailure = workers.stream()
                .filter(w -> w instanceof SmsWorker)
                .mapToLong(MessageWorker::getFailureCount).sum();
        long mmsSuccess = workers.stream()
                .filter(w -> w instanceof MmsWorker)
                .mapToLong(MessageWorker::getSuccessCount).sum();
        long mmsFailure = workers.stream()
                .filter(w -> w instanceof MmsWorker)
                .mapToLong(MessageWorker::getFailureCount).sum();
        long pushSuccess = workers.stream()
                .filter(w -> w instanceof PushWorker)
                .mapToLong(MessageWorker::getSuccessCount).sum();
        long pushFailure = workers.stream()
                .filter(w -> w instanceof PushWorker)
                .mapToLong(MessageWorker::getFailureCount).sum();

        return new WorkerStats(smsSuccess, smsFailure, mmsSuccess, mmsFailure, pushSuccess, pushFailure);
    }

    public record WorkerStats(
            long smsSuccess, long smsFailure,
            long mmsSuccess, long mmsFailure,
            long pushSuccess, long pushFailure
    ) {
        public long totalSuccess() { return smsSuccess + mmsSuccess + pushSuccess; }
        public long totalFailure() { return smsFailure + mmsFailure + pushFailure; }
    }
}
