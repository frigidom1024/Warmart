package com.mall.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mall.order.entity.LogisticsTrack;
import com.mall.order.mapper.LogisticsTrackMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LogisticsService {

    private final LogisticsTrackMapper logisticsTrackMapper;

    @Transactional
    public LogisticsTrack addTrack(Long orderId, String status, String message, String location, LocalDateTime trackTime) {
        LogisticsTrack track = new LogisticsTrack();
        track.setOrderId(orderId);
        track.setStatus(status);
        track.setMessage(message);
        track.setLocation(location);
        track.setTrackTime(trackTime != null ? trackTime : LocalDateTime.now());
        track.setCreatedTime(LocalDateTime.now());
        logisticsTrackMapper.insert(track);
        return track;
    }

    public List<LogisticsTrack> getTracks(Long orderId) {
        return logisticsTrackMapper.selectList(
                new LambdaQueryWrapper<LogisticsTrack>()
                        .eq(LogisticsTrack::getOrderId, orderId)
                        .orderByDesc(LogisticsTrack::getTrackTime));
    }

    @Transactional
    public void updateTrack(Long id, String status, String message, String location, LocalDateTime trackTime) {
        LogisticsTrack track = logisticsTrackMapper.selectById(id);
        if (track == null) {
            throw new RuntimeException("Logistics track not found");
        }
        if (status != null) track.setStatus(status);
        if (message != null) track.setMessage(message);
        if (location != null) track.setLocation(location);
        if (trackTime != null) track.setTrackTime(trackTime);
        logisticsTrackMapper.updateById(track);
    }

    @Transactional
    public void deleteTrack(Long id) {
        LogisticsTrack track = logisticsTrackMapper.selectById(id);
        if (track == null) {
            throw new RuntimeException("Logistics track not found");
        }
        logisticsTrackMapper.deleteById(id);
    }
}
