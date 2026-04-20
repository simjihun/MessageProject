package com.messaging.queue;

import com.messaging.model.BaseMessage;
import com.messaging.model.MessageStatus;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Store & Forward 패턴의 핵심: 메시지 큐
 *
 * 실제 SMSC/MMS/Push Center 모두 이 패턴을 사용:
 * 1. Store: 메시지 수신 즉시 큐에 저장 (유실 방지)
 * 2. Forward: Worker가 꺼내서 전송 처리
 *
 * LinkedBlockingQueue: Thread-safe, Producer-Consumer 패턴에 최적
 */
public class MessageQueue<T extends BaseMessage> {

    private final String name;
    private final BlockingQueue<T> queue;
    private final AtomicLong totalEnqueued = new AtomicLong(0);
    private final AtomicLong totalDequeued = new AtomicLong(0);

    public MessageQueue(String name, int capacity) {
        this.name = name;
        this.queue = new LinkedBlockingQueue<>(capacity);
    }

    /**
     * Store: 메시지를 큐에 저장
     * 큐가 꽉 찬 경우 최대 5초 대기 후 false 반환 (Back-pressure)
     */
    public boolean enqueue(T message) throws InterruptedException {
        boolean offered = queue.offer(message, 5, TimeUnit.SECONDS);
        if (offered) {
            message.updateStatus(MessageStatus.QUEUED);
            totalEnqueued.incrementAndGet();
        }
        return offered;
    }

    /**
     * Forward: 큐에서 메시지 꺼내기
     * 메시지가 없으면 최대 1초 대기 (blocking)
     */
    public T dequeue() throws InterruptedException {
        T message = queue.poll(1, TimeUnit.SECONDS);
        if (message != null) {
            message.updateStatus(MessageStatus.PROCESSING);
            totalDequeued.incrementAndGet();
        }
        return message;
    }

    public int size() { return queue.size(); }
    public String getName() { return name; }
    public long getTotalEnqueued() { return totalEnqueued.get(); }
    public long getTotalDequeued() { return totalDequeued.get(); }
    public boolean isEmpty() { return queue.isEmpty(); }

    @Override
    public String toString() {
        return String.format("Queue[%s] size=%d enqueued=%d dequeued=%d",
                name, size(), totalEnqueued.get(), totalDequeued.get());
    }
}
