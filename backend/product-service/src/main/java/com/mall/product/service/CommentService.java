package com.mall.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.product.dto.CommentVO;
import com.mall.product.entity.Comment;
import com.mall.product.mapper.CommentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentMapper commentMapper;
    private final RestTemplate restTemplate;

    public void add(Long productId, Long userId, String content, Integer rating, String imageUrls) {
        Comment comment = new Comment();
        comment.setProductId(productId);
        comment.setUserId(userId);
        comment.setContent(content);
        comment.setRating(rating);
        comment.setImageUrls(imageUrls);
        comment.setCreatedTime(LocalDateTime.now());
        commentMapper.insert(comment);
    }

    public IPage<CommentVO> listByProductId(Long productId, int page, int size) {
        Page<Comment> commentPage = commentMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Comment>()
                        .eq(Comment::getProductId, productId)
                        .orderByDesc(Comment::getCreatedTime));

        Page<CommentVO> voPage = new Page<>(commentPage.getCurrent(), commentPage.getSize(), commentPage.getTotal());
        if (commentPage.getRecords().isEmpty()) {
            voPage.setRecords(Collections.emptyList());
            return voPage;
        }

        List<Long> userIds = commentPage.getRecords().stream()
                .map(Comment::getUserId).distinct().collect(Collectors.toList());

        Map<Long, Map<String, Object>> finalUserMap;
        try {
            String url = "http://auth-service:8081/api/auth/user/batch?ids=" +
                    userIds.stream().map(String::valueOf).collect(Collectors.joining(","));
            var response = restTemplate.getForEntity(url, Map.class);
            Map<String, Object> result = response.getBody();
            if (result != null && Integer.valueOf(200).equals(result.get("code"))) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) result.get("data");
                if (data != null) {
                    finalUserMap = data.entrySet().stream()
                            .collect(Collectors.toMap(
                                    e -> Long.valueOf(e.getKey()),
                                    e -> {
                                        Map<String, Object> val = (Map<String, Object>) e.getValue();
                                        return val;
                                    }));
                } else {
                    finalUserMap = Collections.emptyMap();
                }
            } else {
                finalUserMap = Collections.emptyMap();
            }
        } catch (Exception ignored) {
            finalUserMap = Collections.emptyMap();
        }

        Map<Long, Map<String, Object>> userMap = finalUserMap;

        List<CommentVO> vos = commentPage.getRecords().stream().map(c -> {
            CommentVO vo = new CommentVO();
            vo.setId(c.getId());
            vo.setProductId(c.getProductId());
            vo.setUserId(c.getUserId());
            vo.setContent(c.getContent());
            vo.setRating(c.getRating());
            vo.setImageUrls(c.getImageUrls());
            vo.setCreatedTime(c.getCreatedTime());

            Map<String, Object> userInfo = userMap.get(c.getUserId());
            if (userInfo != null) {
                vo.setUserNickname((String) userInfo.getOrDefault("nickname", "用户"));
                vo.setUserAvatar((String) userInfo.get("avatar"));
            } else {
                vo.setUserNickname("用户");
            }
            return vo;
        }).collect(Collectors.toList());

        voPage.setRecords(vos);
        return voPage;
    }
}
