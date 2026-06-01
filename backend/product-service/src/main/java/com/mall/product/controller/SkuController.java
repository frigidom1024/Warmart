package com.mall.product.controller;

import com.mall.product.common.Result;
import com.mall.product.entity.*;
import com.mall.product.service.SkuService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class SkuController {

    private final SkuService skuService;

    @GetMapping("/sku/list/{productId}")
    public Result<List<ProductSku>> skuList(@PathVariable Long productId) {
        return Result.success(skuService.getSkus(productId));
    }

    @PutMapping("/admin/spec-groups")
    public Result<Void> saveSpecGroups(@RequestBody Map<String, Object> body) {
        Long productId = Long.valueOf(body.get("productId").toString());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> groups = (List<Map<String, Object>>) body.get("groups");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> skus = (List<Map<String, Object>>) body.get("skus");
        skuService.saveGroupsAndSkus(productId, groups, skus);
        return Result.success(null);
    }
}
