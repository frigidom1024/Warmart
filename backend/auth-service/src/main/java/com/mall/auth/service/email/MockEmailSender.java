package com.mall.auth.service.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "email.sender-type", havingValue = "mock", matchIfMissing = true)
public class MockEmailSender implements EmailSender {

    @Override
    public void send(String to, String subject, String content) {
        log.info("[Mock邮件] ==============================");
        log.info("[Mock邮件] 收件人: {}", to);
        log.info("[Mock邮件] 主题: {}", subject);
        log.info("[Mock邮件] 内容: {}", content);
        log.info("[Mock邮件] ==============================");
    }
}
