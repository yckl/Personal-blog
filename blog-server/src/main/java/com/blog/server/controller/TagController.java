package com.blog.server.controller;

import com.blog.server.common.Result;
import com.blog.server.entity.ArticleTag;
import com.blog.server.exception.BusinessException;
import com.blog.server.mapper.ArticleTagMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagController {

    private final ArticleTagMapper tagMapper;

    @GetMapping
    public Result<List<ArticleTag>> list() {
        return Result.ok(tagMapper.selectList(new LambdaQueryWrapper<>()));
    }

    @PostMapping
    public Result<Long> create(@RequestBody TagRequest request) {
        ArticleTag tag = new ArticleTag();
        tag.setName(request.getName());
        tag.setSlug(request.getSlug());
        tag.setColor(request.getColor());
        tagMapper.insert(tag);
        return Result.ok(tag.getId());
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody TagRequest request) {
        ArticleTag tag = tagMapper.selectById(id);
        if (tag == null) throw new BusinessException(404, "Tag not found");
        tag.setName(request.getName());
        tag.setSlug(request.getSlug());
        tag.setColor(request.getColor());
        tagMapper.updateById(tag);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        tagMapper.deleteById(id);
        return Result.ok();
    }

    @Data
    public static class TagRequest {
        private String name;
        private String slug;
        private String color;
    }
}
