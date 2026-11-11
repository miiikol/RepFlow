package com.reqflow.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_role_permission")
public class SysRolePermission {

    @TableId(value = "role_id", type = IdType.INPUT)
    private Long roleId;
    private Long permissionId;
}
