package com.mall.user.controller;

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
}
