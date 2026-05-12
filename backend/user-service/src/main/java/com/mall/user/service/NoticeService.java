package com.mall.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mall.user.entity.Notice;
import com.mall.user.mapper.NoticeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NoticeService {

    private final NoticeMapper noticeMapper;

    public List<Notice> listPublished(String type) {
        LambdaQueryWrapper<Notice> wrapper = new LambdaQueryWrapper<Notice>()
                .eq(Notice::getStatus, 0)
                .orderByDesc(Notice::getCreatedTime);
        if (type != null && !type.isEmpty()) {
            wrapper.eq(Notice::getType, type);
        }
        return noticeMapper.selectList(wrapper);
    }
}
