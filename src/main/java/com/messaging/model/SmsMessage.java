package com.messaging.model;

/**
 * SMS 메시지 - SMSC(Short Message Service Center) 처리 대상
 * 3G/LTE/5G 네트워크에서 MAP/Diameter 프로토콜로 전달됨
 * MT(Mobile Terminated): 서버→단말, MO(Mobile Originated): 단말→서버
 */
public class SmsMessage extends BaseMessage {

    public enum Direction { MT, MO }
    public enum NetworkType { G3, LTE, G5 }

    private final String sender;
    private final String recipient;
    private final String content;        // 최대 140바이트 (한글 70자)
    private final Direction direction;
    private final NetworkType network;

    private SmsMessage(Builder builder) {
        super(MessageType.SMS);
        this.sender = builder.sender;
        this.recipient = builder.recipient;
        this.content = builder.content;
        this.direction = builder.direction;
        this.network = builder.network;
    }

    public String getSender() { return sender; }
    public String getRecipient() { return recipient; }
    public String getContent() { return content; }
    public Direction getDirection() { return direction; }
    public NetworkType getNetwork() { return network; }

    @Override
    public String toString() {
        return String.format("%s sender=%s -> recipient=%s [%s/%s] \"%s\"",
                super.toString(), sender, recipient, direction, network,
                content.length() > 20 ? content.substring(0, 20) + "..." : content);
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String sender;
        private String recipient;
        private String content;
        private Direction direction = Direction.MT;
        private NetworkType network = NetworkType.LTE;

        public Builder sender(String sender) { this.sender = sender; return this; }
        public Builder recipient(String recipient) { this.recipient = recipient; return this; }
        public Builder content(String content) { this.content = content; return this; }
        public Builder direction(Direction direction) { this.direction = direction; return this; }
        public Builder network(NetworkType network) { this.network = network; return this; }
        public SmsMessage build() { return new SmsMessage(this); }
    }
}
