package com.mall.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.product.entity.Comment;
import com.mall.product.mapper.CommentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentMapper commentMapper;

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

    public IPage<Comment> listByProductId(Long productId, int page, int size) {
        return commentMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Comment>()
                        .eq(Comment::getProductId, productId)
                        .orderByDesc(Comment::getCreatedTime));
    }
}
