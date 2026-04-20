package com.messaging.controller;

/**
 * REST API 요청 DTO
 */
public class MessageRequest {

    // 공통
    private String type;      // SMS / MMS / PUSH
    private String sender;
    private String recipient; // SMS/MMS: 전화번호, PUSH: device token

    // SMS
    private String content;
    private String network;   // G3 / LTE / G5
    private String direction; // MT / MO

    // MMS
    private String subject;
    private String textContent;

    // PUSH
    private String platform;  // IOS / ANDROID
    private String title;
    private String body;
    private int badge;

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }
    public String getRecipient() { return recipient; }
    public void setRecipient(String recipient) { this.recipient = recipient; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getNetwork() { return network; }
    public void setNetwork(String network) { this.network = network; }
    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getTextContent() { return textContent; }
    public void setTextContent(String textContent) { this.textContent = textContent; }
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public int getBadge() { return badge; }
    public void setBadge(int badge) { this.badge = badge; }
}
