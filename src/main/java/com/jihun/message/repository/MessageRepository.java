package com.jihun.message.repository;

import com.jihun.message.domain.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Spring Data JPA가 구현체를 자동으로 만들어주는 저장소 인터페이스.
 */
public interface MessageRepository extends JpaRepository<Message, Long> {

    /** 최신 접수 순으로 최대 50건 조회 (화면 목록용) */
    List<Message> findTop50ByOrderByIdDesc();
}
