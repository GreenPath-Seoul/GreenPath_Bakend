package com.seoul.greenpath.domain.member.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 회원 도메인 이벤트 리스너
 */
@Slf4j
@Component
public class MemberEventListener {

    /**
     * 회원 가입 트랜잭션이 성공적으로 커밋된 직후 비동기로 실행됨
     * ex) 가입 축하 이메일 발송, 초기 포인트 지급, 알림 전송 등
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMemberRegisteredEvent(MemberRegisteredEvent event) {
        log.info("[Event] 🚀 회원가입 완료. 환영 로직 실행 - memberId: {}, email: {}", event.getMemberId(), event.getEmail());
        // TODO: EmailService.sendWelcomeEmail(event.getEmail(), event.getName());
    }
}
