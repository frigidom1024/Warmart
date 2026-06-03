package com.mall.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.product.dto.CommentVO;
import com.mall.product.entity.Comment;
import com.mall.product.entity.Product;
import com.mall.product.mapper.CommentMapper;
import com.mall.product.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(CommentService.class);

    private final CommentMapper commentMapper;
    private final ProductMapper productMapper;
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

        Map<Long, Map<String, Object>> userMap = resolveUserInfo(userIds);

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

    public IPage<CommentVO> adminList(int page, int size, String productName, String username, Integer rating) {
        // 1. Resolve product IDs by name at DB level via ProductMapper
        List<Long> filterProductIds = null;
        if (productName != null && !productName.isEmpty()) {
            List<Product> products = productMapper.selectList(
                    new LambdaQueryWrapper<Product>()
                            .like(Product::getName, productName)
                            .select(Product::getId));
            filterProductIds = products.stream().map(Product::getId).collect(Collectors.toList());
            if (filterProductIds.isEmpty()) {
                Page<CommentVO> emptyPage = new Page<>(page, size, 0);
                emptyPage.setRecords(Collections.emptyList());
                return emptyPage;
            }
        }

        // 2. Resolve user IDs by nickname at DB level via auth-service REST
        List<Long> filterUserIds = null;
        if (username != null && !username.isEmpty()) {
            try {
                String url = "http://auth-service:8081/api/auth/user/search?nickname=" + username;
                var response = restTemplate.getForEntity(url, Map.class);
                Map<String, Object> result = response.getBody();
                if (result != null && Integer.valueOf(200).equals(result.get("code"))) {
                    @SuppressWarnings("unchecked")
                    List<Object> data = (List<Object>) result.get("data");
                    if (data != null) {
                        filterUserIds = data.stream()
                                .map(o -> Long.valueOf(o.toString()))
                                .collect(Collectors.toList());
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to search users by nickname from auth-service: {}", e.getMessage());
            }
            if (filterUserIds == null || filterUserIds.isEmpty()) {
                Page<CommentVO> emptyPage = new Page<>(page, size, 0);
                emptyPage.setRecords(Collections.emptyList());
                return emptyPage;
            }
        }

        // 3. Build DB query with all filters applied at DB level
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<Comment>()
                .orderByDesc(Comment::getCreatedTime);
        if (rating != null) {
            wrapper.eq(Comment::getRating, rating);
        }
        if (filterProductIds != null) {
            wrapper.in(Comment::getProductId, filterProductIds);
        }
        if (filterUserIds != null) {
            wrapper.in(Comment::getUserId, filterUserIds);
        }

        // 4. Execute paginated query (total comes from DB, correct pagination metadata)
        Page<Comment> commentPage = commentMapper.selectPage(new Page<>(page, size), wrapper);

        Page<CommentVO> voPage = new Page<>(commentPage.getCurrent(), commentPage.getSize(), commentPage.getTotal());
        if (commentPage.getRecords().isEmpty()) {
            voPage.setRecords(Collections.emptyList());
            return voPage;
        }

        // 5. Resolve user info for the returned page's comments
        List<Long> userIdsInPage = commentPage.getRecords().stream()
                .map(Comment::getUserId).distinct().collect(Collectors.toList());
        Map<Long, Map<String, Object>> userMap = resolveUserInfo(userIdsInPage);

        // 6. Resolve product names for the returned page's comments
        List<Long> productIdsInPage = commentPage.getRecords().stream()
                .map(Comment::getProductId).distinct().collect(Collectors.toList());
        Map<Long, String> productNameMap;
        if (!productIdsInPage.isEmpty()) {
            List<Product> products = productMapper.selectBatchIds(productIdsInPage);
            productNameMap = products.stream()
                    .collect(Collectors.toMap(Product::getId, Product::getName, (a, b) -> a));
        } else {
            productNameMap = Collections.emptyMap();
        }

        // 7. Map to VOs with resolved user and product info
        List<CommentVO> vos = commentPage.getRecords().stream().map(c -> {
            CommentVO vo = new CommentVO();
            vo.setId(c.getId());
            vo.setProductId(c.getProductId());
            vo.setUserId(c.getUserId());
            vo.setContent(c.getContent());
            vo.setRating(c.getRating());
            vo.setImageUrls(c.getImageUrls());
            vo.setCreatedTime(c.getCreatedTime());
            vo.setProductName(productNameMap.getOrDefault(c.getProductId(), ""));

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

    /**
     * Resolve user display info (nickname, avatar) from auth-service by user IDs.
     */
    private Map<Long, Map<String, Object>> resolveUserInfo(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            String url = "http://auth-service:8081/api/auth/user/batch?ids=" +
                    userIds.stream().map(String::valueOf).collect(Collectors.joining(","));
            var response = restTemplate.getForEntity(url, Map.class);
            Map<String, Object> result = response.getBody();
            if (result != null && Integer.valueOf(200).equals(result.get("code"))) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) result.get("data");
                if (data != null) {
                    return data.entrySet().stream()
                            .collect(Collectors.toMap(
                                    e -> Long.valueOf(e.getKey()),
                                    e -> (Map<String, Object>) e.getValue()));
                }
            }
        } catch (Exception e) {
            log.warn("Failed to resolve user info from auth-service: {}", e.getMessage());
        }
        return Collections.emptyMap();
    }

    public void deleteById(Long id) {
        commentMapper.deleteById(id);
    }
}
