package com.messaging.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 모든 메시지 타입의 공통 기반 클래스
 * SMS / MMS / Push 모두 Store & Forward 패턴을 따름
 */
public abstract class BaseMessage {

    private final String messageId;
    private final MessageType type;
    private MessageStatus status;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int retryCount;
    private static final int MAX_RETRY = 3;

    // CDR(Call Detail Record)용 필드 - 과금/통계 목적
    private LocalDateTime sentAt;
    private LocalDateTime deliveredAt;

    protected BaseMessage(MessageType type) {
        this.messageId = UUID.randomUUID().toString();
        this.type = type;
        this.status = MessageStatus.RECEIVED;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.retryCount = 0;
    }

    public void updateStatus(MessageStatus newStatus) {
        this.status = newStatus;
        this.updatedAt = LocalDateTime.now();
        if (newStatus == MessageStatus.SENT) {
            this.sentAt = LocalDateTime.now();
        } else if (newStatus == MessageStatus.DELIVERED) {
            this.deliveredAt = LocalDateTime.now();
        }
    }

    public boolean canRetry() {
        return retryCount < MAX_RETRY;
    }

    public void incrementRetry() {
        retryCount++;
        this.status = MessageStatus.RETRYING;
        this.updatedAt = LocalDateTime.now();
    }

    public String getMessageId() { return messageId; }
    public MessageType getType() { return type; }
    public MessageStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public int getRetryCount() { return retryCount; }
    public LocalDateTime getSentAt() { return sentAt; }
    public LocalDateTime getDeliveredAt() { return deliveredAt; }

    @Override
    public String toString() {
        return String.format("[%s] id=%s status=%s retry=%d",
                type, messageId.substring(0, 8), status, retryCount);
    }
}
