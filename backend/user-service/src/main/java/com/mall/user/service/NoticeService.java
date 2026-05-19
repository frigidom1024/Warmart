package com.mall.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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

    public IPage<Notice> listPublishedPage(String type, int page, int size) {
        LambdaQueryWrapper<Notice> wrapper = new LambdaQueryWrapper<Notice>()
                .eq(Notice::getStatus, 0)
                .orderByDesc(Notice::getCreatedTime);
        if (type != null && !type.isEmpty()) {
            wrapper.eq(Notice::getType, type);
        }
        return noticeMapper.selectPage(new Page<>(page, size), wrapper);
    }

    public Notice detail(Long id) {
        return noticeMapper.selectById(id);
    }
}
