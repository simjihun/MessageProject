package com.messaging.monitor;

import com.messaging.queue.MessageQueueManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 큐 상태 모니터링 - 실시간 통계 출력
 * 실제 시스템의 Admin WEB / 모니터링 SW 역할
 *
 * @Scheduled: Spring의 스케줄러로 주기적으로 실행
 */
@Component
public class QueueMonitor {

    private static final Logger log = LoggerFactory.getLogger(QueueMonitor.class);

    private final MessageQueueManager queueManager;
    private final List<CdrRecord> cdrStore = Collections.synchronizedList(new ArrayList<>());

    public QueueMonitor(MessageQueueManager queueManager) {
        this.queueManager = queueManager;
    }

    /** 5초마다 큐 상태 로그 출력 */
    @Scheduled(fixedDelay = 5000)
    public void printQueueStats() {
        log.info("=== QUEUE MONITOR ===");
        log.info("SMS  Queue: size={} totalIn={} totalOut={}",
                queueManager.getSmsQueue().size(),
                queueManager.getSmsQueue().getTotalEnqueued(),
                queueManager.getSmsQueue().getTotalDequeued());
        log.info("MMS  Queue: size={} totalIn={} totalOut={}",
                queueManager.getMmsQueue().size(),
                queueManager.getMmsQueue().getTotalEnqueued(),
                queueManager.getMmsQueue().getTotalDequeued());
        log.info("Push Queue: size={} totalIn={} totalOut={}",
                queueManager.getPushQueue().size(),
                queueManager.getPushQueue().getTotalEnqueued(),
                queueManager.getPushQueue().getTotalDequeued());
    }

    public void addCdr(CdrRecord record) {
        cdrStore.add(record);
        if (cdrStore.size() > 10_000) {
            cdrStore.subList(0, 1000).clear(); // 오래된 CDR 제거
        }
    }

    public List<CdrRecord> getRecentCdrs(int limit) {
        int size = cdrStore.size();
        int from = Math.max(0, size - limit);
        return new ArrayList<>(cdrStore.subList(from, size));
    }

    public long getTotalCdrs() { return cdrStore.size(); }
}
