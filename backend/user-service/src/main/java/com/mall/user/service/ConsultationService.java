package com.mall.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mall.user.entity.Consultation;
import com.mall.user.mapper.ConsultationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConsultationService {

    private final ConsultationMapper consultationMapper;

    public void add(Long userId, Long productId, String question) {
        Consultation consultation = new Consultation();
        consultation.setUserId(userId);
        consultation.setProductId(productId);
        consultation.setQuestion(question);
        consultation.setStatus(0);
        consultation.setCreatedTime(LocalDateTime.now());
        consultationMapper.insert(consultation);
    }

    public List<Consultation> listByProductId(Long productId) {
        return consultationMapper.selectList(
                new LambdaQueryWrapper<Consultation>()
                        .eq(Consultation::getProductId, productId)
                        .orderByDesc(Consultation::getCreatedTime));
    }

    public List<Consultation> listByUserId(Long userId) {
        return consultationMapper.selectList(
                new LambdaQueryWrapper<Consultation>()
                        .eq(Consultation::getUserId, userId)
                        .orderByDesc(Consultation::getCreatedTime));
    }

    public List<Consultation> adminList() {
        return consultationMapper.selectList(
                new LambdaQueryWrapper<Consultation>()
                        .orderByDesc(Consultation::getCreatedTime));
    }

    public void reply(Long id, String answer) {
        Consultation consultation = consultationMapper.selectById(id);
        if (consultation != null) {
            consultation.setAnswer(answer);
            consultation.setStatus(1);
            consultationMapper.updateById(consultation);
        }
    }
}
