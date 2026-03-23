package com.blog.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.server.entity.VisitLog;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface VisitLogMapper extends BaseMapper<VisitLog> {

    @Select("SELECT COUNT(DISTINCT ip_address) FROM visit_log WHERE created_at >= #{since}")
    long countUniqueVisitorsSince(@Param("since") LocalDateTime since);

    @Select("SELECT DATE(created_at) AS day, COUNT(*) AS cnt FROM visit_log " +
            "WHERE created_at >= #{since} GROUP BY DATE(created_at) ORDER BY day")
    List<Map<String, Object>> selectDailyPV(@Param("since") LocalDateTime since);

    @Select("SELECT DATE(created_at) AS day, COUNT(DISTINCT ip_address) AS cnt FROM visit_log " +
            "WHERE created_at >= #{since} GROUP BY DATE(created_at) ORDER BY day")
    List<Map<String, Object>> selectDailyUV(@Param("since") LocalDateTime since);
}
