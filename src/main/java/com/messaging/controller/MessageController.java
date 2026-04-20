package com.messaging.controller;

import com.messaging.model.*;
import com.messaging.processor.WorkerPoolManager;
import com.messaging.queue.MessageQueueManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 메시지 플랫폼 REST API
 *
 * POST /api/messages/sms   - SMS 전송 요청
 * POST /api/messages/mms   - MMS 전송 요청
 * POST /api/messages/push  - Push 알림 전송 요청
 * GET  /api/messages/stats - 처리 통계 조회
 */
@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageQueueManager queueManager;
    private final WorkerPoolManager workerPoolManager;

    public MessageController(MessageQueueManager queueManager, WorkerPoolManager workerPoolManager) {
        this.queueManager = queueManager;
        this.workerPoolManager = workerPoolManager;
    }

    @PostMapping("/sms")
    public ResponseEntity<Map<String, Object>> sendSms(@RequestBody MessageRequest req) throws InterruptedException {
        SmsMessage msg = SmsMessage.builder()
                .sender(req.getSender() != null ? req.getSender() : "01000000000")
                .recipient(req.getRecipient())
                .content(req.getContent())
                .network(parseNetwork(req.getNetwork()))
                .direction(parseDirection(req.getDirection()))
                .build();

        boolean queued = queueManager.getSmsQueue().enqueue(msg);
        return queued
                ? ResponseEntity.ok(Map.of("messageId", msg.getMessageId(), "status", "QUEUED"))
                : ResponseEntity.status(503).body(Map.of("error", "SMS queue full"));
    }

    @PostMapping("/mms")
    public ResponseEntity<Map<String, Object>> sendMms(@RequestBody MessageRequest req) throws InterruptedException {
        MmsMessage msg = MmsMessage.builder()
                .sender(req.getSender() != null ? req.getSender() : "01000000000")
                .recipient(req.getRecipient())
                .subject(req.getSubject() != null ? req.getSubject() : "")
                .textContent(req.getTextContent() != null ? req.getTextContent() : "")
                .build();

        boolean queued = queueManager.getMmsQueue().enqueue(msg);
        return queued
                ? ResponseEntity.ok(Map.of("messageId", msg.getMessageId(), "status", "QUEUED"))
                : ResponseEntity.status(503).body(Map.of("error", "MMS queue full"));
    }

    @PostMapping("/push")
    public ResponseEntity<Map<String, Object>> sendPush(@RequestBody MessageRequest req) throws InterruptedException {
        PushMessage msg = PushMessage.builder()
                .deviceToken(req.getRecipient())
                .platform(parsePlatform(req.getPlatform()))
                .title(req.getTitle() != null ? req.getTitle() : "")
                .body(req.getBody() != null ? req.getBody() : "")
                .badge(req.getBadge())
                .build();

        boolean queued = queueManager.getPushQueue().enqueue(msg);
        return queued
                ? ResponseEntity.ok(Map.of("messageId", msg.getMessageId(), "status", "QUEUED"))
                : ResponseEntity.status(503).body(Map.of("error", "Push queue full"));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        var workerStats = workerPoolManager.getStats();
        return ResponseEntity.ok(Map.of(
                "queues", Map.of(
                        "sms",  Map.of("size", queueManager.getSmsQueue().size(),
                                       "totalEnqueued", queueManager.getSmsQueue().getTotalEnqueued()),
                        "mms",  Map.of("size", queueManager.getMmsQueue().size(),
                                       "totalEnqueued", queueManager.getMmsQueue().getTotalEnqueued()),
                        "push", Map.of("size", queueManager.getPushQueue().size(),
                                       "totalEnqueued", queueManager.getPushQueue().getTotalEnqueued())
                ),
                "workers", Map.of(
                        "smsSuccess",  workerStats.smsSuccess(),
                        "smsFailure",  workerStats.smsFailure(),
                        "mmsSuccess",  workerStats.mmsSuccess(),
                        "mmsFailure",  workerStats.mmsFailure(),
                        "pushSuccess", workerStats.pushSuccess(),
                        "pushFailure", workerStats.pushFailure(),
                        "totalSuccess", workerStats.totalSuccess(),
                        "totalFailure", workerStats.totalFailure()
                )
        ));
    }

    private SmsMessage.NetworkType parseNetwork(String network) {
        if (network == null) return SmsMessage.NetworkType.LTE;
        return switch (network.toUpperCase()) {
            case "3G", "G3" -> SmsMessage.NetworkType.G3;
            case "5G", "G5" -> SmsMessage.NetworkType.G5;
            default -> SmsMessage.NetworkType.LTE;
        };
    }

    private SmsMessage.Direction parseDirection(String dir) {
        return "MO".equalsIgnoreCase(dir) ? SmsMessage.Direction.MO : SmsMessage.Direction.MT;
    }

    private PushMessage.Platform parsePlatform(String platform) {
        return "ANDROID".equalsIgnoreCase(platform) ? PushMessage.Platform.ANDROID : PushMessage.Platform.IOS;
    }
}
