package com.reqflow.vo;

import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
public class UserVO {

    private Long id;
    private String username;
    private String realName;
    private Long deptId;
    private String deptName;
    private String email;
    private String phone;
    private Integer status;
    private Integer mustChangePwd;
    private List<String> roles;
    private List<Long> roleIds;
    private Set<String> permissions;
    private String dataScope;
}
