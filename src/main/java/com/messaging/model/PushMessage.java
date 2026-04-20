package com.messaging.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Push 메시지 - Push Center 처리 대상
 * iOS: APNS(Apple Push Notification Service) TCP/IP(TLS)
 * Android: FCM/GCM HTTPS(SSL)
 * 다이어그램의 Push Center → Push GW → Send D 흐름
 */
public class PushMessage extends BaseMessage {

    public enum Platform { IOS, ANDROID }

    private final String deviceToken;   // APNS device token 또는 FCM registration token
    private final Platform platform;
    private final String title;
    private final String body;
    private final Map<String, Object> data; // 커스텀 페이로드
    private final int badge;                // iOS 앱 아이콘 배지 숫자
    private final String sound;

    private PushMessage(Builder builder) {
        super(MessageType.PUSH);
        this.deviceToken = builder.deviceToken;
        this.platform = builder.platform;
        this.title = builder.title;
        this.body = builder.body;
        this.data = Map.copyOf(builder.data);
        this.badge = builder.badge;
        this.sound = builder.sound;
    }

    public String getDeviceToken() { return deviceToken; }
    public Platform getPlatform() { return platform; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public Map<String, Object> getData() { return data; }
    public int getBadge() { return badge; }
    public String getSound() { return sound; }

    /** APNS 전송용 JSON 페이로드 생성 (실제 구현에서는 JWT 인증 필요) */
    public String toApnsPayload() {
        return String.format("""
                {"aps":{"alert":{"title":"%s","body":"%s"},"badge":%d,"sound":"%s"}}""",
                title, body, badge, sound);
    }

    /** FCM 전송용 JSON 페이로드 생성 */
    public String toFcmPayload() {
        return String.format("""
                {"to":"%s","notification":{"title":"%s","body":"%s"}}""",
                deviceToken, title, body);
    }

    @Override
    public String toString() {
        return String.format("%s platform=%s token=%s... title=\"%s\"",
                super.toString(), platform,
                deviceToken.length() > 8 ? deviceToken.substring(0, 8) : deviceToken,
                title);
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String deviceToken;
        private Platform platform;
        private String title = "";
        private String body = "";
        private Map<String, Object> data = new HashMap<>();
        private int badge = 0;
        private String sound = "default";

        public Builder deviceToken(String t) { this.deviceToken = t; return this; }
        public Builder platform(Platform p) { this.platform = p; return this; }
        public Builder title(String t) { this.title = t; return this; }
        public Builder body(String b) { this.body = b; return this; }
        public Builder data(String key, Object value) { this.data.put(key, value); return this; }
        public Builder badge(int b) { this.badge = b; return this; }
        public Builder sound(String s) { this.sound = s; return this; }
        public PushMessage build() { return new PushMessage(this); }
    }
}
