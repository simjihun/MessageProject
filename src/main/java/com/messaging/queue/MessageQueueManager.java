package com.messaging.queue;

import com.messaging.model.MmsMessage;
import com.messaging.model.PushMessage;
import com.messaging.model.SmsMessage;
import org.springframework.stereotype.Component;

/**
 * 메시지 타입별 큐 관리
 * SMS / MMS / Push 각각 독립적인 큐로 분리 → 서로 영향 없이 처리
 *
 * 실제 시스템에서는 Kafka / RabbitMQ / ActiveMQ 등을 사용하지만
 * 이 프로젝트는 Java 내장 BlockingQueue로 동일한 개념을 구현
 */
@Component
public class MessageQueueManager {

    private static final int SMS_QUEUE_CAPACITY  = 100_000;
    private static final int MMS_QUEUE_CAPACITY  = 10_000;
    private static final int PUSH_QUEUE_CAPACITY = 500_000;

    private final MessageQueue<SmsMessage>  smsQueue;
    private final MessageQueue<MmsMessage>  mmsQueue;
    private final MessageQueue<PushMessage> pushQueue;

    public MessageQueueManager() {
        this.smsQueue  = new MessageQueue<>("SMS-Queue",  SMS_QUEUE_CAPACITY);
        this.mmsQueue  = new MessageQueue<>("MMS-Queue",  MMS_QUEUE_CAPACITY);
        this.pushQueue = new MessageQueue<>("Push-Queue", PUSH_QUEUE_CAPACITY);
    }

    public MessageQueue<SmsMessage>  getSmsQueue()  { return smsQueue; }
    public MessageQueue<MmsMessage>  getMmsQueue()  { return mmsQueue; }
    public MessageQueue<PushMessage> getPushQueue() { return pushQueue; }

    public String getStats() {
        return String.format("""
                === Queue Stats ===
                %s
                %s
                %s""",
                smsQueue, mmsQueue, pushQueue);
    }
}
