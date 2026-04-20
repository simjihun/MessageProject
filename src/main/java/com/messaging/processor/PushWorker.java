package com.messaging.processor;

import com.messaging.model.PushMessage;
import com.messaging.queue.MessageQueue;

import java.util.Random;

/**
 * Push Worker: APNS(iOS) / FCM(Android) 전송 처리
 * 다이어그램의 Push Center → APNS I/F, FCM(GCM) I/F 역할
 *
 * 실제 구현 시:
 * - APNS: HTTP/2 + JWT 인증, p8 키 파일 필요
 * - FCM: HTTP v1 API + OAuth2 Service Account
 */
public class PushWorker extends MessageWorker<PushMessage> {

    private final Random random = new Random();

    public PushWorker(MessageQueue<PushMessage> queue) {
        super(queue);
    }

    @Override
    protected boolean send(PushMessage message) throws Exception {
        return switch (message.getPlatform()) {
            case IOS     -> sendViaApns(message);
            case ANDROID -> sendViaFcm(message);
        };
    }

    private boolean sendViaApns(PushMessage message) throws InterruptedException {
        // APNS는 TCP/IP(TLS) 영구 연결 방식 - 지연이 매우 짧음
        Thread.sleep(5 + random.nextInt(20));
        String payload = message.toApnsPayload();
        log.debug("APNS payload sent: {}", payload);

        // APNS 특이사항: 디바이스 토큰이 만료된 경우 410 응답
        if (random.nextInt(20) == 0) {
            log.warn("APNS: device token expired for {}", message.getDeviceToken());
            return false;
        }
        return true;
    }

    private boolean sendViaFcm(PushMessage message) throws InterruptedException {
        // FCM은 HTTPS(SSL) 방식 - APNS보다 약간 느림
        Thread.sleep(20 + random.nextInt(50));
        String payload = message.toFcmPayload();
        log.debug("FCM payload sent: {}", payload);

        // FCM: 등록 토큰 유효하지 않은 경우
        if (random.nextInt(15) == 0) {
            log.warn("FCM: invalid registration token for {}", message.getDeviceToken());
            return false;
        }
        return true;
    }
}
