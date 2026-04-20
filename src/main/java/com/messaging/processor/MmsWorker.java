package com.messaging.processor;

import com.messaging.model.MmsMessage;
import com.messaging.queue.MessageQueue;

import java.util.Random;

/**
 * MMS Worker: MMS Relay/Server 처리
 * 대용량 첨부파일 → 전송 시간이 SMS보다 길다
 * MM4 인터페이스: 외부 MMS 서버(이메일/FAX/UMS)로 라우팅
 */
public class MmsWorker extends MessageWorker<MmsMessage> {

    private static final long MAX_CONTENT_SIZE = 10 * 1024 * 1024; // 10MB
    private final Random random = new Random();

    public MmsWorker(MessageQueue<MmsMessage> queue) {
        super(queue);
    }

    @Override
    protected boolean send(MmsMessage message) throws Exception {
        // 콘텐츠 크기 검증
        if (message.getTotalSizeBytes() > MAX_CONTENT_SIZE) {
            log.warn("MMS rejected: content size {}MB exceeds limit",
                    message.getTotalSizeBytes() / (1024 * 1024));
            return false;
        }

        // 파일 크기에 비례한 전송 지연 시뮬레이션
        long sizeKb = message.getTotalSizeBytes() / 1024;
        long latencyMs = 100 + (sizeKb * 2); // 기본 100ms + 1KB당 2ms
        Thread.sleep(Math.min(latencyMs, 3000)); // 최대 3초

        // MM4(외부 MMS 서버) 라우팅 시뮬레이션
        if (message.getMmInterface() == MmsMessage.MmInterface.MM4) {
            log.debug("MMS routed via MM4 (external server): {}", message);
        }

        if (random.nextInt(10) == 0) return false;

        log.debug("MMS sent in {}ms, size={}KB: {}", latencyMs, sizeKb, message);
        return true;
    }
}
