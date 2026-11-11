package com.reqflow.security;

import com.reqflow.domain.entity.SysRole;
import com.reqflow.domain.entity.SysUser;
import com.reqflow.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final SysUserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = userMapper.selectByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }
        return toLoginUser(user);
    }

    public LoginUser toLoginUser(SysUser user) {
        List<SysRole> roles = userMapper.selectRoles(user.getId());
        List<String> roleCodes = roles.stream().map(SysRole::getCode).toList();
        String dataScope = roles.stream()
                .map(SysRole::getDataScope)
                .min((a, b) -> Integer.compare(scopeRank(a), scopeRank(b)))
                .orElse("SELF");
        LoginUser loginUser = new LoginUser();
        loginUser.setId(user.getId());
        loginUser.setUsername(user.getUsername());
        loginUser.setPassword(user.getPassword());
        loginUser.setRealName(user.getRealName());
        loginUser.setDeptId(user.getDeptId());
        loginUser.setStatus(user.getStatus());
        loginUser.setMustChangePwd(user.getMustChangePwd());
        loginUser.setRoles(roleCodes);
        loginUser.setPermissions(new HashSet<>(userMapper.selectPermissionCodes(user.getId())));
        loginUser.setDataScope(dataScope);
        loginUser.setLockUntil(user.getLockUntil());
        return loginUser;
    }

    private int scopeRank(String scope) {
        return switch (scope == null ? "" : scope) {
            case "ALL" -> 0;
            case "DEPT" -> 1;
            case "PROJECT" -> 2;
            default -> 3;
        };
    }
}
