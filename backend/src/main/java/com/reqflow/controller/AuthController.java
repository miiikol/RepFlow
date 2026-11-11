package com.reqflow.controller;

import com.reqflow.common.Result;
import com.reqflow.dto.ChangePasswordRequest;
import com.reqflow.dto.LoginRequest;
import com.reqflow.dto.LoginResponse;
import com.reqflow.service.AuthService;
import com.reqflow.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(authService.login(request));
    }

    @GetMapping("/me")
    public Result<UserVO> me() {
        return Result.ok(authService.me());
    }

    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request.getOldPassword(), request.getNewPassword());
        return Result.ok();
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.ok();
    }
}
