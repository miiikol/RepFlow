package com.reqflow.service;

import com.reqflow.common.BizException;
import com.reqflow.common.ErrorCode;
import com.reqflow.common.SecurityUtils;
import com.reqflow.domain.entity.SysUser;
import com.reqflow.dto.LoginRequest;
import com.reqflow.dto.LoginResponse;
import com.reqflow.mapper.SysUserMapper;
import com.reqflow.security.JwtService;
import com.reqflow.security.LoginUser;
import com.reqflow.security.UserDetailsServiceImpl;
import com.reqflow.service.support.LookupService;
import com.reqflow.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final SysUserMapper userMapper;
    private final UserDetailsServiceImpl userDetailsService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final LookupService lookupService;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        SysUser user = userMapper.selectByUsername(request.getUsername());
        if (user == null) {
            throw BizException.of(ErrorCode.BAD_CREDENTIALS);
        }
        if (user.getLockUntil() != null && user.getLockUntil().isAfter(LocalDateTime.now())) {
            throw BizException.of(ErrorCode.ACCOUNT_LOCKED);
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw BizException.of(ErrorCode.ACCOUNT_DISABLED);
        }
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
            LoginUser loginUser = (LoginUser) authentication.getPrincipal();
            user.setFailCount(0);
            user.setLockUntil(null);
            userMapper.updateById(user);
            String token = jwtService.generate(loginUser);
            return new LoginResponse(token, "Bearer", jwtService.getExpireMinutes() * 60, toVO(loginUser));
        } catch (LockedException e) {
            throw BizException.of(ErrorCode.ACCOUNT_LOCKED);
        } catch (DisabledException e) {
            throw BizException.of(ErrorCode.ACCOUNT_DISABLED);
        } catch (BadCredentialsException e) {
            int fails = (user.getFailCount() == null ? 0 : user.getFailCount()) + 1;
            user.setFailCount(fails);
            if (fails >= 5) {
                user.setLockUntil(LocalDateTime.now().plusMinutes(15));
            }
            userMapper.updateById(user);
            throw fails >= 5
                    ? BizException.of(ErrorCode.ACCOUNT_LOCKED, "连续 5 次密码错误，账号锁定 15 分钟")
                    : BizException.of(ErrorCode.BAD_CREDENTIALS,
                    "用户名或密码错误，还可尝试 " + (5 - fails) + " 次");
        }
    }

    public UserVO me() {
        return toVO(SecurityUtils.currentUser());
    }

    public UserVO toVO(LoginUser loginUser) {
        UserVO vo = new UserVO();
        vo.setId(loginUser.getId());
        vo.setUsername(loginUser.getUsername());
        vo.setRealName(loginUser.getRealName());
        vo.setDeptId(loginUser.getDeptId());
        vo.setDeptName(lookupService.deptName(loginUser.getDeptId()));
        vo.setRoles(loginUser.getRoles());
        vo.setPermissions(loginUser.getPermissions());
        vo.setDataScope(loginUser.getDataScope());
        vo.setMustChangePwd(loginUser.getMustChangePwd());
        vo.setRoleIds(userMapper.selectRoles(loginUser.getId()).stream().map(r -> r.getId()).toList());
        return vo;
    }

    @Transactional
    public void changePassword(String oldPassword, String newPassword) {
        Long userId = SecurityUtils.userId();
        SysUser user = userMapper.selectById(userId);
        if (user == null || !passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BizException("原密码不正确");
        }
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new BizException("新密码不能与原密码相同");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setMustChangePwd(0);
        userMapper.updateById(user);
    }
}
