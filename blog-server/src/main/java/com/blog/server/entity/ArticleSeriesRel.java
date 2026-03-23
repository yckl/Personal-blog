package com.blog.server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("article_series_rel")
public class ArticleSeriesRel {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long articleId;
    private Long seriesId;
    private Integer sortOrder;
}
