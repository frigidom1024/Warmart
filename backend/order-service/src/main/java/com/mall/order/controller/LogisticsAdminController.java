package com.mall.order.controller;

import com.mall.order.common.Result;
import com.mall.order.entity.LogisticsTrack;
import com.mall.order.service.LogisticsService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/order/admin/logistics")
@RequiredArgsConstructor
public class LogisticsAdminController {

    private final LogisticsService logisticsService;

    @PostMapping("/track")
    public Result<Void> addTrack(@RequestBody AddTrackRequest request) {
        logisticsService.addTrack(request.getOrderId(), request.getStatus(),
                request.getMessage(), request.getLocation(),
                request.getTrackTime() != null ? LocalDateTime.parse(request.getTrackTime()) : null);
        return Result.success(null);
    }

    @GetMapping("/tracks/{orderId}")
    public Result<List<LogisticsTrack>> getTracks(@PathVariable Long orderId) {
        return Result.success(logisticsService.getTracks(orderId));
    }

    @PutMapping("/track/{id}")
    public Result<Void> updateTrack(@PathVariable Long id, @RequestBody AddTrackRequest request) {
        logisticsService.updateTrack(id, request.getStatus(),
                request.getMessage(), request.getLocation(),
                request.getTrackTime() != null ? LocalDateTime.parse(request.getTrackTime()) : null);
        return Result.success(null);
    }

    @DeleteMapping("/track/{id}")
    public Result<Void> deleteTrack(@PathVariable Long id) {
        logisticsService.deleteTrack(id);
        return Result.success(null);
    }

    @Data
    public static class AddTrackRequest {
        private Long orderId;
        private String status;
        private String message;
        private String location;
        private String trackTime;
    }
}
