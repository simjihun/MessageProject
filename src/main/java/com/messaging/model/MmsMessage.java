package com.messaging.model;

import java.util.ArrayList;
import java.util.List;

/**
 * MMS 메시지 - MMS Relay/Server 처리 대상
 * MM1~MM8 인터페이스로 구분 (다이어그램 참조)
 * MM1: UA↔Relay, MM2: Relay↔Server, MM4: Server↔Foreign MMS Server
 */
public class MmsMessage extends BaseMessage {

    public enum MmInterface {
        MM1,  // MMS User Agent ↔ MMS Relay
        MM2,  // MMS Relay ↔ MMS Server
        MM4,  // MMS Server ↔ Foreign MMS Relay/Server
        MM7   // MMS VAS Application ↔ MMS Relay/Server
    }

    public record Attachment(String filename, String mimeType, long sizeBytes) {}

    private final String sender;
    private final String recipient;
    private final String subject;
    private final String textContent;
    private final List<Attachment> attachments;
    private final MmInterface mmInterface;

    private MmsMessage(Builder builder) {
        super(MessageType.MMS);
        this.sender = builder.sender;
        this.recipient = builder.recipient;
        this.subject = builder.subject;
        this.textContent = builder.textContent;
        this.attachments = List.copyOf(builder.attachments);
        this.mmInterface = builder.mmInterface;
    }

    public String getSender() { return sender; }
    public String getRecipient() { return recipient; }
    public String getSubject() { return subject; }
    public String getTextContent() { return textContent; }
    public List<Attachment> getAttachments() { return attachments; }
    public MmInterface getMmInterface() { return mmInterface; }

    public long getTotalSizeBytes() {
        return attachments.stream().mapToLong(Attachment::sizeBytes).sum();
    }

    @Override
    public String toString() {
        return String.format("%s sender=%s -> recipient=%s [%s] subject=\"%s\" attachments=%d totalSize=%dKB",
                super.toString(), sender, recipient, mmInterface, subject,
                attachments.size(), getTotalSizeBytes() / 1024);
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String sender;
        private String recipient;
        private String subject = "";
        private String textContent = "";
        private List<Attachment> attachments = new ArrayList<>();
        private MmInterface mmInterface = MmInterface.MM1;

        public Builder sender(String s) { this.sender = s; return this; }
        public Builder recipient(String r) { this.recipient = r; return this; }
        public Builder subject(String s) { this.subject = s; return this; }
        public Builder textContent(String t) { this.textContent = t; return this; }
        public Builder attachment(String filename, String mime, long bytes) {
            this.attachments.add(new Attachment(filename, mime, bytes));
            return this;
        }
        public Builder mmInterface(MmInterface i) { this.mmInterface = i; return this; }
        public MmsMessage build() { return new MmsMessage(this); }
    }
}
