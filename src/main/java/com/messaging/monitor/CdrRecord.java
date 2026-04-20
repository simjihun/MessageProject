package com.messaging.monitor;

import java.time.LocalDateTime;

/**
 * CDR (Call Detail Record) - 과금/통계용 레코드
 * 실제 통신사 시스템에서 모든 메시지에 대해 생성됨
 * 과금 시스템(Billing), 스팸 필터링, 분석에 활용
 */
public record CdrRecord(
        String messageId,
        String type,
        String sender,
        String recipient,
        LocalDateTime receivedAt,
        LocalDateTime sentAt,
        long processingTimeMs,
        boolean success,
        int retryCount,
        String failureReason
) {
    @Override
    public String toString() {
        return String.format("CDR[%s] %s %s->%s success=%s retry=%d elapsed=%dms",
                messageId.substring(0, 8), type, sender, recipient,
                success, retryCount, processingTimeMs);
    }
}
