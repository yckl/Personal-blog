package com.blog.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.server.dto.response.RecentComment;
import com.blog.server.entity.Comment;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface CommentMapper extends BaseMapper<Comment> {

    @Select("SELECT c.id, c.article_id, a.title AS article_title, " +
            "c.author_name, c.author_avatar, c.content, c.status, c.created_at " +
            "FROM comment c LEFT JOIN article a ON c.article_id = a.id " +
            "WHERE c.deleted = 0 ORDER BY c.created_at DESC LIMIT #{limit}")
    List<RecentComment> selectRecentWithArticleTitle(@Param("limit") int limit);
}
