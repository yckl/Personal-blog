package com.blog.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.server.entity.ArticleHistory;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ArticleHistoryMapper extends BaseMapper<ArticleHistory> {
}
