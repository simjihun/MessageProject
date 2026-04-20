package com.messaging;

import com.messaging.model.*;
import com.messaging.queue.MessageQueue;
import com.messaging.queue.MessageQueueManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class MessageQueueTest {

    @Test
    @DisplayName("SMS 메시지 Store & Forward 기본 동작")
    void smsStoreAndForward() throws InterruptedException {
        MessageQueue<SmsMessage> queue = new MessageQueue<>("test-sms", 100);

        SmsMessage msg = SmsMessage.builder()
                .sender("01011111111")
                .recipient("01022222222")
                .content("안녕하세요 테스트 메시지입니다")
                .network(SmsMessage.NetworkType.LTE)
                .build();

        // Store
        boolean stored = queue.enqueue(msg);
        assertTrue(stored, "메시지가 큐에 저장되어야 함");
        assertEquals(MessageStatus.QUEUED, msg.getStatus());
        assertEquals(1, queue.size());

        // Forward
        SmsMessage dequeued = queue.dequeue();
        assertNotNull(dequeued, "큐에서 메시지를 꺼낼 수 있어야 함");
        assertEquals(MessageStatus.PROCESSING, dequeued.getStatus());
        assertEquals(msg.getMessageId(), dequeued.getMessageId());
        assertEquals(0, queue.size());
    }

    @Test
    @DisplayName("Push 메시지 플랫폼별 분기 (iOS/Android)")
    void pushMessagePlatformRouting() throws InterruptedException {
        MessageQueueManager manager = new MessageQueueManager();

        PushMessage iosMsg = PushMessage.builder()
                .deviceToken("abc123def456")
                .platform(PushMessage.Platform.IOS)
                .title("새 알림")
                .body("iOS 테스트 푸시")
                .badge(1)
                .build();

        PushMessage androidMsg = PushMessage.builder()
                .deviceToken("xyz789ghi012")
                .platform(PushMessage.Platform.ANDROID)
                .title("새 알림")
                .body("Android 테스트 푸시")
                .build();

        manager.getPushQueue().enqueue(iosMsg);
        manager.getPushQueue().enqueue(androidMsg);

        assertEquals(2, manager.getPushQueue().size());

        // APNS 페이로드 검증
        String apnsPayload = iosMsg.toApnsPayload();
        assertTrue(apnsPayload.contains("\"aps\""), "APNS 페이로드 형식 오류");
        assertTrue(apnsPayload.contains("badge"), "APNS badge 필드 누락");

        // FCM 페이로드 검증
        String fcmPayload = androidMsg.toFcmPayload();
        assertTrue(fcmPayload.contains("\"to\""), "FCM 페이로드 형식 오류");
        assertTrue(fcmPayload.contains("xyz789ghi012"), "FCM device token 누락");
    }

    @Test
    @DisplayName("대용량 동시 처리 - Producer/Consumer 패턴")
    void concurrentProducerConsumer() throws InterruptedException {
        MessageQueue<SmsMessage> queue = new MessageQueue<>("load-test", 10_000);
        int messageCount = 1000;
        AtomicInteger producedCount = new AtomicInteger(0);
        AtomicInteger consumedCount = new AtomicInteger(0);
        CountDownLatch producerDone = new CountDownLatch(1);

        // Producer: 1000개 메시지 생성
        ExecutorService producerPool = Executors.newFixedThreadPool(5);
        for (int i = 0; i < messageCount; i++) {
            final int idx = i;
            producerPool.submit(() -> {
                try {
                    SmsMessage msg = SmsMessage.builder()
                            .sender("01011111111")
                            .recipient("010" + String.format("%08d", idx))
                            .content("대량 발송 테스트 #" + idx)
                            .build();
                    queue.enqueue(msg);
                    producedCount.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        producerPool.shutdown();
        producerPool.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS);
        producerDone.countDown();

        // Consumer: 모두 꺼내기
        while (!queue.isEmpty()) {
            SmsMessage msg = queue.dequeue();
            if (msg != null) consumedCount.incrementAndGet();
        }

        assertEquals(messageCount, producedCount.get(), "1000개 메시지 모두 생성되어야 함");
        assertEquals(producedCount.get(), consumedCount.get(), "생산량 = 소비량");
        assertEquals(messageCount, queue.getTotalEnqueued());
    }

    @Test
    @DisplayName("MMS 파일 크기 및 첨부파일 검증")
    void mmsAttachmentValidation() {
        MmsMessage msg = MmsMessage.builder()
                .sender("01011111111")
                .recipient("01022222222")
                .subject("사진 보내기")
                .textContent("첨부 사진 확인해주세요")
                .attachment("photo.jpg", "image/jpeg", 2 * 1024 * 1024L) // 2MB
                .attachment("video.mp4", "video/mp4", 5 * 1024 * 1024L)  // 5MB
                .mmInterface(MmsMessage.MmInterface.MM1)
                .build();

        assertEquals(2, msg.getAttachments().size());
        assertEquals(7 * 1024 * 1024L, msg.getTotalSizeBytes());
        assertEquals(MessageType.MMS, msg.getType());
        assertEquals(MessageStatus.RECEIVED, msg.getStatus());
    }

    @Test
    @DisplayName("재전송 로직 - 최대 횟수 초과 시 FAILED")
    void retryExhausted() {
        SmsMessage msg = SmsMessage.builder()
                .sender("01011111111")
                .recipient("01022222222")
                .content("재전송 테스트")
                .build();

        assertTrue(msg.canRetry());
        msg.incrementRetry(); // 1회
        assertEquals(MessageStatus.RETRYING, msg.getStatus());
        assertTrue(msg.canRetry());
        msg.incrementRetry(); // 2회
        assertTrue(msg.canRetry());
        msg.incrementRetry(); // 3회
        assertFalse(msg.canRetry(), "3회 재시도 후에는 재전송 불가");
    }
}
