package com.mall.auth.service.email;

/**
 * 邮件发送抽象接口。
 * 业务层通过此接口发送邮件，与具体供应商解耦。
 * 切换供应商只需新增实现类并修改 email.sender-type 配置。
 */
public interface EmailSender {

    /**
     * 发送邮件
     *
     * @param to      收件人邮箱
     * @param subject 邮件主题
     * @param content 邮件正文（纯文本）
     */
    void send(String to, String subject, String content);
}
