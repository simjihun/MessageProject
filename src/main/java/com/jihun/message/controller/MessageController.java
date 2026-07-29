package com.jihun.message.controller;

import com.jihun.message.controller.dto.MessageSendRequest;
import com.jihun.message.domain.Message;
import com.jihun.message.queue.MessageQueue;
import com.jihun.message.repository.MessageRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 메시지 발송 REST API.
 *
 * 핵심 설계: API는 발송을 "직접" 하지 않는다.
 * DB에 저장하고 큐에 넣은 뒤 즉시 응답한다(비동기 처리).
 * 실제 발송은 백그라운드 워커가 담당한다.
 * → 발송이 느려도 API 응답 속도에 영향이 없다.
 */
@RestController
public class MessageController {

    private final MessageRepository messageRepository;
    private final MessageQueue messageQueue;

    public MessageController(MessageRepository messageRepository, MessageQueue messageQueue) {
        this.messageRepository = messageRepository;
        this.messageQueue = messageQueue;
    }

    /** 메시지 발송 요청 접수 */
    @PostMapping("/api/messages")
    public ResponseEntity<Message> send(@Valid @RequestBody MessageSendRequest request) {
        Message message = new Message(request.receiver(), request.content());
        messageRepository.save(message);   // 1. 이력을 DB에 먼저 저장 (PENDING)
        messageQueue.enqueue(message.getId()); // 2. 큐에 ID를 적재 → 워커가 처리
        return ResponseEntity.ok(message);
    }

    /** 발송 이력 조회 (최신 50건) */
    @GetMapping("/api/messages")
    public List<Message> list() {
        return messageRepository.findTop50ByOrderByIdDesc();
    }

    /** 현재 대기열 상태 */
    @GetMapping("/api/queue/status")
    public Map<String, Object> queueStatus() {
        return Map.of("waiting", messageQueue.size());
    }

    /**
     * 헬스체크 엔드포인트.
     * 나중에 AWS 로드밸런서(ALB)나 ECS가 "이 서버 살아있나?"를 확인할 때 사용한다.
     */
    @GetMapping("/healthz")
    public Map<String, String> healthz() {
        return Map.of("status", "ok");
    }
}
