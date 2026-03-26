package com.blog.server.controller;

import com.blog.server.common.Result;
import com.blog.server.entity.ArticleCategory;
import com.blog.server.entity.Article;
import com.blog.server.exception.BusinessException;
import com.blog.server.mapper.ArticleCategoryMapper;
import com.blog.server.mapper.ArticleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final ArticleCategoryMapper categoryMapper;
    private final ArticleMapper articleMapper;

    @GetMapping
    public Result<List<ArticleCategory>> list() {
        return Result.ok(categoryMapper.selectList(
                new LambdaQueryWrapper<ArticleCategory>().orderByAsc(ArticleCategory::getSortOrder)));
    }

    @PostMapping
    public Result<Long> create(@Valid @RequestBody CategoryRequest request) {
        // Check slug uniqueness
        if (request.getSlug() != null) {
            Long exists = categoryMapper.selectCount(
                    new LambdaQueryWrapper<ArticleCategory>().eq(ArticleCategory::getSlug, request.getSlug()));
            if (exists > 0) throw new BusinessException(400, "Category slug already exists");
        }
        ArticleCategory category = new ArticleCategory();
        category.setName(request.getName());
        category.setSlug(request.getSlug());
        category.setDescription(request.getDescription());
        category.setParentId(request.getParentId());
        category.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        categoryMapper.insert(category);
        return Result.ok(category.getId());
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        ArticleCategory category = categoryMapper.selectById(id);
        if (category == null) throw new BusinessException(404, "Category not found");
        category.setName(request.getName());
        category.setSlug(request.getSlug());
        category.setDescription(request.getDescription());
        category.setParentId(request.getParentId());
        if (request.getSortOrder() != null) category.setSortOrder(request.getSortOrder());
        categoryMapper.updateById(category);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        // Check if any articles reference this category
        Long articleCount = articleMapper.selectCount(
                new LambdaQueryWrapper<Article>().eq(Article::getCategoryId, id));
        if (articleCount > 0) {
            throw new BusinessException(400, "Cannot delete category: " + articleCount
                    + " article(s) still reference it. Please reassign them first.");
        }
        categoryMapper.deleteById(id);
        return Result.ok();
    }

    @Data
    public static class CategoryRequest {
        @NotBlank(message = "Category name is required")
        private String name;
        private String slug;
        private String description;
        private Long parentId;
        private Integer sortOrder;
    }
}
