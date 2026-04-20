package com.messaging.model;

public enum MessageStatus {
    RECEIVED,    // 수신 완료 (Store)
    QUEUED,      // 큐 대기중
    PROCESSING,  // 처리중
    SENT,        // 전송 완료 (Forward)
    DELIVERED,   // 수신자 전달 완료
    FAILED,      // 전송 실패
    RETRYING     // 재전송 중
}
