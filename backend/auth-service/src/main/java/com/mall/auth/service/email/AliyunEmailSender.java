package com.mall.auth.service.email;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.dm.model.v20151123.SingleSendMailRequest;
import com.aliyuncs.dm.model.v20151123.SingleSendMailResponse;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.profile.DefaultProfile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "email.sender-type", havingValue = "aliyun")
public class AliyunEmailSender implements EmailSender {

    private final IAcsClient client;
    private final String accountName;
    private final Integer addressType;
    private final Boolean replyToAddress;

    public AliyunEmailSender(
            @Value("${aliyun.dm.access-key-id}") String accessKeyId,
            @Value("${aliyun.dm.access-key-secret}") String accessKeySecret,
            @Value("${aliyun.dm.account-name}") String accountName,
            @Value("${aliyun.dm.address-type:1}") Integer addressType,
            @Value("${aliyun.dm.reply-to-address:true}") Boolean replyToAddress
    ) {
        this.accountName = accountName;
        this.addressType = addressType;
        this.replyToAddress = replyToAddress;
        DefaultProfile profile = DefaultProfile.getProfile("cn-hangzhou", accessKeyId, accessKeySecret);
        this.client = new DefaultAcsClient(profile);
    }

    @Override
    public void send(String to, String subject, String content) {
        SingleSendMailRequest request = new SingleSendMailRequest();
        request.setAccountName(accountName);
        request.setAddressType(addressType);
        request.setReplyToAddress(replyToAddress);
        request.setToAddress(to);
        request.setSubject(subject);
        request.setTextBody(content);

        try {
            SingleSendMailResponse response = client.getAcsResponse(request);
            log.info("邮件发送成功 -> to={}, subject={}, envId={}", to, subject, response.getEnvId());
        } catch (ClientException e) {
            log.error("邮件发送失败 -> to={}, subject={}, errCode={}, errMsg={}",
                    to, subject, e.getErrCode(), e.getErrMsg());
            throw new RuntimeException("邮件发送失败: " + e.getErrMsg(), e);
        }
    }
}
