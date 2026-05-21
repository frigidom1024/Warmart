package com.mall.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mall.user.entity.UserFeedback;
import com.mall.user.mapper.UserFeedbackMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserFeedbackService {

    private final UserFeedbackMapper feedbackMapper;

    public void add(Long userId, String type, String content) {
        UserFeedback feedback = new UserFeedback();
        feedback.setUserId(userId);
        feedback.setType(type);
        feedback.setContent(content);
        feedback.setStatus(0);
        feedback.setCreatedTime(LocalDateTime.now());
        feedbackMapper.insert(feedback);
    }

    public List<UserFeedback> listByUserId(Long userId) {
        return feedbackMapper.selectList(
                new LambdaQueryWrapper<UserFeedback>()
                        .eq(UserFeedback::getUserId, userId)
                        .orderByDesc(UserFeedback::getCreatedTime));
    }

    public List<UserFeedback> adminList() {
        return feedbackMapper.selectList(
                new LambdaQueryWrapper<UserFeedback>()
                        .orderByDesc(UserFeedback::getCreatedTime));
    }

    public void reply(Long id, String replyContent) {
        UserFeedback feedback = feedbackMapper.selectById(id);
        if (feedback != null) {
            feedback.setReply(replyContent);
            feedback.setReplyTime(LocalDateTime.now());
            feedback.setStatus(1);
            feedbackMapper.updateById(feedback);
        }
    }
}
