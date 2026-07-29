package com.jihun.message.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 발송 요청 바디.
 * record: 불변 데이터 전달용 클래스를 간단하게 정의하는 Java 문법 (Java 16+)
 */
public record MessageSendRequest(

        @NotBlank(message = "수신 번호는 필수입니다")
        @Size(max = 20)
        String receiver,

        @NotBlank(message = "메시지 내용은 필수입니다")
        @Size(max = 500)
        String content
) {
}
