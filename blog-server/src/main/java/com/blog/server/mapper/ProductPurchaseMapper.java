package com.blog.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.server.entity.ProductPurchase;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProductPurchaseMapper extends BaseMapper<ProductPurchase> {
}
