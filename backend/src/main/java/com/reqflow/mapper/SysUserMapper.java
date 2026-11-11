package com.reqflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reqflow.domain.entity.SysUser;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface SysUserMapper extends BaseMapper<SysUser> {

    @Select("""
            select p.code from sys_permission p
            join sys_role_permission rp on p.id = rp.permission_id
            join sys_user_role ur on ur.role_id = rp.role_id
            where ur.user_id = #{userId}
            """)
    List<String> selectPermissionCodes(Long userId);

    @Select("""
            select r.code from sys_role r
            join sys_user_role ur on ur.role_id = r.id
            where ur.user_id = #{userId}
            """)
    List<String> selectRoleCodes(Long userId);

    @Select("""
            select r.* from sys_role r
            join sys_user_role ur on ur.role_id = r.id
            where ur.user_id = #{userId}
            """)
    List<com.reqflow.domain.entity.SysRole> selectRoles(Long userId);

    @Select("select * from sys_user where username = #{username} and deleted = 0 limit 1")
    SysUser selectByUsername(String username);
}
