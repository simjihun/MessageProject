package com.messaging.model;

public enum MessageType {
    SMS,   // Short Message Service - SMSC 처리
    MMS,   // Multimedia Message Service - MMS Relay/Server 처리
    PUSH   // App Push - APNS(iOS) / FCM(Android) 처리
}
