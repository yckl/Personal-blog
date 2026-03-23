package com.blog.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.server.entity.PaymentOrder;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PaymentOrderMapper extends BaseMapper<PaymentOrder> {
}
