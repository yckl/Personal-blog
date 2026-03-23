package com.blog.server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("article_tag_rel")
public class ArticleTagRel {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long articleId;
    private Long tagId;
}
