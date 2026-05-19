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
}
