package com.messaging.processor;

import com.messaging.model.SmsMessage;
import com.messaging.queue.MessageQueue;

import java.util.Random;

/**
 * SMSC Worker: SMS 메시지 전송 처리
 * 실제로는 MAP(SS7) 또는 Diameter 프로토콜로 HLR/HSS 조회 후 MSC/SGSN/MME/SMSF에 전달
 * 네트워크 타입(3G/LTE/5G)별로 다른 노드에 라우팅
 */
public class SmsWorker extends MessageWorker<SmsMessage> {

    private final Random random = new Random();

    public SmsWorker(MessageQueue<SmsMessage> queue) {
        super(queue);
    }

    @Override
    protected boolean send(SmsMessage message) throws Exception {
        // 전송 시간 시뮬레이션 (네트워크 지연)
        long latencyMs = switch (message.getNetwork()) {
            case G3  -> 50 + random.nextInt(100);  // 3G: 50~150ms
            case LTE -> 10 + random.nextInt(30);   // LTE: 10~40ms
            case G5  -> 5  + random.nextInt(15);   // 5G: 5~20ms
        };
        Thread.sleep(latencyMs);

        // 실패율 시뮬레이션: 10% 확률로 실패 (재전송 로직 테스트용)
        if (random.nextInt(10) == 0) {
            return false;
        }

        log.debug("SMS sent via {} in {}ms: {}", message.getNetwork(), latencyMs, message);
        return true;
    }
}
