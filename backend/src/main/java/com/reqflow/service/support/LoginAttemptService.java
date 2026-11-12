package com.reqflow.service.support;

import com.reqflow.domain.entity.SysUser;
import com.reqflow.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 登录失败计数。
 * <p>
 * 计数必须与“抛异常”解耦：若和登录放在同一事务里，抛异常会把 fail_count 一起回滚，
 * 账号锁定永远不会生效，因此这里固定使用独立事务（REQUIRES_NEW）并直接提交。
 */
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    /** 连续失败达到该次数后锁定 */
    public static final int MAX_FAILS = 5;
    /** 锁定时长（分钟） */
    public static final int LOCK_MINUTES = 15;

    private final SysUserMapper userMapper;

    /** 记录一次密码错误，返回累计失败次数；达到阈值时同时写入锁定时间 */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int recordFailure(Long userId) {
        userMapper.increaseFailCount(userId);
        SysUser user = userMapper.selectById(userId);
        int fails = user == null || user.getFailCount() == null ? 0 : user.getFailCount();
        if (fails >= MAX_FAILS) {
            userMapper.updateLockUntil(userId, LocalDateTime.now().plusMinutes(LOCK_MINUTES));
        }
        return fails;
    }

    /** 登录成功后清零计数与锁定时间 */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void reset(Long userId) {
        userMapper.resetFailCount(userId);
    }
}
