package com.mall.order.controller;

import com.mall.order.common.Result;
import com.mall.order.entity.LogisticsTrack;
import com.mall.order.service.LogisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/order/logistics")
@RequiredArgsConstructor
public class LogisticsController {

    private final LogisticsService logisticsService;

    @GetMapping("/tracks/{orderId}")
    public Result<List<LogisticsTrack>> getTracks(@PathVariable Long orderId) {
        return Result.success(logisticsService.getTracks(orderId));
    }
}
