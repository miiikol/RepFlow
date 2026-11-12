package com.reqflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reqflow.domain.entity.SysUser;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
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

    /** 原子自增失败次数，避免“先查后改”的并发覆盖 */
    @Update("update sys_user set fail_count = fail_count + 1, updated_at = CURRENT_TIMESTAMP where id = #{userId}")
    int increaseFailCount(@Param("userId") Long userId);

    @Update("update sys_user set fail_count = 0, lock_until = null, updated_at = CURRENT_TIMESTAMP where id = #{userId}")
    int resetFailCount(@Param("userId") Long userId);

    @Update("update sys_user set lock_until = #{lockUntil}, updated_at = CURRENT_TIMESTAMP where id = #{userId}")
    int updateLockUntil(@Param("userId") Long userId, @Param("lockUntil") LocalDateTime lockUntil);
}
