package com.blog.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.server.dto.response.HotArticle;
import com.blog.server.entity.Article;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface ArticleMapper extends BaseMapper<Article> {

    @Select("SELECT id, title, slug, cover_image, view_count, like_count, comment_count, status " +
            "FROM article WHERE deleted = 0 " +
            "ORDER BY view_count DESC LIMIT #{limit}")
    List<HotArticle> selectHotArticles(@Param("limit") int limit);
}
