package com.mall.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mall.product.entity.Banner;
import com.mall.product.mapper.BannerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BannerService {

    private final BannerMapper bannerMapper;

    public List<Banner> listActive() {
        return bannerMapper.selectList(
                new LambdaQueryWrapper<Banner>()
                        .eq(Banner::getStatus, 0)
                        .orderByAsc(Banner::getSort));
    }

    public List<Banner> listAll() {
        return bannerMapper.selectList(
                new LambdaQueryWrapper<Banner>()
                        .orderByAsc(Banner::getSort));
    }

    public void add(Banner banner) {
        banner.setCreatedTime(LocalDateTime.now());
        bannerMapper.insert(banner);
    }

    public void update(Banner banner) {
        bannerMapper.updateById(banner);
    }

    public void delete(Long id) {
        bannerMapper.deleteById(id);
    }
}
