package com.blog.server.controller;

import com.blog.server.common.Result;
import com.blog.server.entity.ArticleSeries;
import com.blog.server.entity.ArticleSeriesRel;
import com.blog.server.exception.BusinessException;
import com.blog.server.mapper.ArticleSeriesMapper;
import com.blog.server.mapper.ArticleSeriesRelMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/series")
@RequiredArgsConstructor
public class SeriesController {

    private final ArticleSeriesMapper seriesMapper;
    private final ArticleSeriesRelMapper seriesRelMapper;

    @GetMapping
    public Result<List<ArticleSeries>> list() {
        return Result.ok(seriesMapper.selectList(
                new LambdaQueryWrapper<ArticleSeries>().orderByAsc(ArticleSeries::getSortOrder)));
    }

    @GetMapping("/{id}")
    public Result<ArticleSeries> get(@PathVariable Long id) {
        ArticleSeries series = seriesMapper.selectById(id);
        if (series == null) throw new BusinessException(404, "Series not found");
        return Result.ok(series);
    }

    @PostMapping
    public Result<Long> create(@RequestBody SeriesRequest request) {
        ArticleSeries series = new ArticleSeries();
        series.setName(request.getName());
        series.setSlug(request.getSlug());
        series.setDescription(request.getDescription());
        series.setCoverImage(request.getCoverImage());
        series.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        seriesMapper.insert(series);
        return Result.ok(series.getId());
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody SeriesRequest request) {
        ArticleSeries series = seriesMapper.selectById(id);
        if (series == null) throw new BusinessException(404, "Series not found");
        series.setName(request.getName());
        series.setSlug(request.getSlug());
        series.setDescription(request.getDescription());
        series.setCoverImage(request.getCoverImage());
        if (request.getSortOrder() != null) series.setSortOrder(request.getSortOrder());
        seriesMapper.updateById(series);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        seriesRelMapper.delete(
                new LambdaQueryWrapper<ArticleSeriesRel>().eq(ArticleSeriesRel::getSeriesId, id));
        seriesMapper.deleteById(id);
        return Result.ok();
    }

    @GetMapping("/{id}/articles")
    public Result<List<ArticleSeriesRel>> getSeriesArticles(@PathVariable Long id) {
        return Result.ok(seriesRelMapper.selectList(
                new LambdaQueryWrapper<ArticleSeriesRel>()
                        .eq(ArticleSeriesRel::getSeriesId, id)
                        .orderByAsc(ArticleSeriesRel::getSortOrder)));
    }

    @Data
    public static class SeriesRequest {
        private String name;
        private String slug;
        private String description;
        private String coverImage;
        private Integer sortOrder;
    }
}
