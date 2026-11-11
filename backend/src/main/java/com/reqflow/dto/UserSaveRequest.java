package com.reqflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class UserSaveRequest {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "姓名不能为空")
    private String realName;

    @NotNull(message = "部门不能为空")
    private Long deptId;

    private String email;
    private String phone;
    private Integer status = 1;
    private List<Long> roleIds;
    private String password;
}
