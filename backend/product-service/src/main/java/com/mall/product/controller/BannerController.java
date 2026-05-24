package com.mall.product.controller;

import com.mall.product.common.Result;
import com.mall.product.entity.Banner;
import com.mall.product.service.BannerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    // ─── Admin endpoints ───

    @GetMapping("/banner/admin/list")
    public Result<List<Banner>> adminList() {
        return Result.success(bannerService.listAll());
    }

    @PostMapping("/banner/admin/add")
    public Result<Void> adminAdd(@RequestBody Banner banner) {
        bannerService.add(banner);
        return Result.success(null);
    }

    @PutMapping("/banner/admin/update")
    public Result<Void> adminUpdate(@RequestBody Banner banner) {
        bannerService.update(banner);
        return Result.success(null);
    }

    @DeleteMapping("/banner/admin/delete/{id}")
    public Result<Void> adminDelete(@PathVariable Long id) {
        bannerService.delete(id);
        return Result.success(null);
    }
}
