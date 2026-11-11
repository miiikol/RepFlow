package com.reqflow.service;

import com.reqflow.common.BizException;
import com.reqflow.common.ErrorCode;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.reqflow.domain.entity.Requirement;
import com.reqflow.mapper.RequirementMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 乐观锁回归：两个请求同时修改同一条需求时，后提交者必须失败而不是覆盖。
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RequirementOptimisticLockTest {

    @Autowired
    private RequirementMapper requirementMapper;
    @Autowired
    private RequirementService requirementService;

    @Test
    void staleUpdateIsRejected() {
        // 用两种查询方式读取同一行，拿到两个独立快照（同一 SqlSession 的重复查询会命中一级缓存）
        Requirement first = requirementMapper.selectById(7L);
        Requirement second = requirementMapper.selectOne(new QueryWrapper<Requirement>().eq("id", 7L));

        first.setTitle(first.getTitle() + "-A");
        requirementService.persist(first);

        second.setTitle(second.getTitle() + "-B");
        BizException exception = assertThrows(BizException.class,
                () -> requirementService.persist(second));
        assertEquals(ErrorCode.CONFLICT.getCode(), exception.getCode());

        Requirement latest = requirementMapper.selectById(7L);
        assertEquals(first.getTitle(), latest.getTitle());
    }
}
