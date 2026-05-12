package com.mall.product.controller;

import com.mall.product.common.Result;
import com.mall.product.entity.Banner;
import com.mall.product.service.BannerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class BannerController {

    private final BannerService bannerService;

    @GetMapping("/banner/list")
    public Result<List<Banner>> list() {
        return Result.success(bannerService.listActive());
    }
}
