package com.blog.server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("article_similarity")
public class ArticleSimilarity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long articleId;
    private Long similarArticleId;
    private Double score;
    private String reason;
    private LocalDateTime updatedAt;
}
