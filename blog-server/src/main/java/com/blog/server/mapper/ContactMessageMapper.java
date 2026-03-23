package com.blog.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.server.entity.ContactMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ContactMessageMapper extends BaseMapper<ContactMessage> {
}
