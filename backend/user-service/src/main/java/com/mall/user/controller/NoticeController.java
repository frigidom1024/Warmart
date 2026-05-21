package com.mall.user.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mall.user.common.Result;
import com.mall.user.entity.Notice;
import com.mall.user.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user/notice")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    @GetMapping("/list")
    public Result<List<Notice>> list(@RequestParam(required = false) String type) {
        return Result.success(noticeService.listPublished(type));
    }

    @GetMapping("/page")
    public Result<IPage<Notice>> page(@RequestParam(required = false) String type,
                                      @RequestParam(defaultValue = "1") int page,
                                      @RequestParam(defaultValue = "10") int size) {
        return Result.success(noticeService.listPublishedPage(type, page, size));
    }

    @GetMapping("/detail/{id}")
    public Result<Notice> detail(@PathVariable Long id) {
        Notice notice = noticeService.detail(id);
        if (notice == null) {
            return Result.error(404, "Notice not found");
        }
        return Result.success(notice);
    }

    // ─── Admin endpoints ───

    @GetMapping("/admin/page")
    public Result<IPage<Notice>> adminPage(
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(noticeService.adminPage(type, page, size));
    }

    @PostMapping("/admin/add")
    public Result<Void> adminAdd(@RequestBody Notice notice) {
        noticeService.add(notice);
        return Result.success(null);
    }

    @PutMapping("/admin/update")
    public Result<Void> adminUpdate(@RequestBody Notice notice) {
        noticeService.update(notice);
        return Result.success(null);
    }

    @DeleteMapping("/admin/delete/{id}")
    public Result<Void> adminDelete(@PathVariable Long id) {
        noticeService.delete(id);
        return Result.success(null);
    }
}
