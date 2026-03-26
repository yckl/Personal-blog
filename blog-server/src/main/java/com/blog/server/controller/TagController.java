package com.blog.server.controller;

import com.blog.server.common.Result;
import com.blog.server.entity.ArticleTag;
import com.blog.server.entity.ArticleTagRel;
import com.blog.server.exception.BusinessException;
import com.blog.server.mapper.ArticleTagMapper;
import com.blog.server.mapper.ArticleTagRelMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagController {

    private final ArticleTagMapper tagMapper;
    private final ArticleTagRelMapper tagRelMapper;

    @GetMapping
    public Result<List<ArticleTag>> list() {
        return Result.ok(tagMapper.selectList(new LambdaQueryWrapper<>()));
    }

    @PostMapping
    public Result<Long> create(@Valid @RequestBody TagRequest request) {
        // Check slug uniqueness
        if (request.getSlug() != null) {
            Long exists = tagMapper.selectCount(
                    new LambdaQueryWrapper<ArticleTag>().eq(ArticleTag::getSlug, request.getSlug()));
            if (exists > 0) throw new BusinessException(400, "Tag slug already exists");
        }
        ArticleTag tag = new ArticleTag();
        tag.setName(request.getName());
        tag.setSlug(request.getSlug());
        tag.setColor(request.getColor());
        tagMapper.insert(tag);
        return Result.ok(tag.getId());
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody TagRequest request) {
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
        // Check if any articles reference this tag
        Long relCount = tagRelMapper.selectCount(
                new LambdaQueryWrapper<ArticleTagRel>().eq(ArticleTagRel::getTagId, id));
        if (relCount > 0) {
            throw new BusinessException(400, "Cannot delete tag: " + relCount
                    + " article(s) still reference it. Please remove the tag from articles first.");
        }
        tagMapper.deleteById(id);
        return Result.ok();
    }

    @Data
    public static class TagRequest {
        @NotBlank(message = "Tag name is required")
        private String name;
        private String slug;
        private String color;
    }
}
