package com.reqflow.service.support;

import com.reqflow.domain.entity.SysSequence;
import com.reqflow.mapper.SysSequenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 业务编号生成器：前缀 + 年月 + 四位流水，例如 REQ-202611-0001。
 * <p>
 * 通过“原子自增 + 行锁”取号，事务独立提交，避免先查后改的竞态；
 * 首次使用某个前缀时插入序号行，并发插入冲突后重试自增。
 */
@Component
@RequiredArgsConstructor
public class BizNoGenerator {

    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyyMM");

    private final SysSequenceMapper sequenceMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String next(String prefix) {
        String date = LocalDate.now().format(MONTH);
        String key = prefix + "-" + date;
        if (sequenceMapper.increment(key) == 0) {
            try {
                SysSequence sequence = new SysSequence();
                sequence.setSeqKey(key);
                sequence.setSeqDate(date);
                sequence.setSeqValue(1);
                sequenceMapper.insert(sequence);
            } catch (DuplicateKeyException e) {
                sequenceMapper.increment(key);
            }
        }
        Integer value = sequenceMapper.selectValue(key);
        if (value == null) {
            throw new IllegalStateException("业务编号生成失败：" + key);
        }
        return "%s-%s-%04d".formatted(prefix, date, value);
    }
}
