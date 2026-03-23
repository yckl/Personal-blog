package com.blog.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.server.entity.SysUser;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface SysUserMapper extends BaseMapper<SysUser> {

    @Select("SELECT r.role_key FROM sys_role r " +
            "JOIN sys_user_role ur ON r.id = ur.role_id " +
            "WHERE ur.user_id = #{userId}")
    List<String> selectRoleKeysByUserId(@Param("userId") Long userId);

    @Select("SELECT DISTINCT p.code FROM sys_permission p " +
            "JOIN sys_role_permission rp ON p.id = rp.permission_id " +
            "JOIN sys_user_role ur ON rp.role_id = ur.role_id " +
            "WHERE ur.user_id = #{userId}")
    List<String> selectPermissionCodesByUserId(@Param("userId") Long userId);
}
