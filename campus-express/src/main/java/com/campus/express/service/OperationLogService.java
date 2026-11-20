package com.campus.express.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.express.common.PageResult;
import com.campus.express.entity.OperationLog;
import com.campus.express.mapper.OperationLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 操作日志查询
 */
@Service
@RequiredArgsConstructor
public class OperationLogService {

    private final OperationLogMapper operationLogMapper;

    public void record(Long userId, String module, String action, String detail, String ip) {
        OperationLog log = new OperationLog();
        log.setUserId(userId);
        log.setModule(module);
        log.setAction(action);
        log.setDetail(detail);
        log.setIp(ip);
        operationLogMapper.insert(log);
    }

    public PageResult<OperationLog> list(
            int page,
            int pageSize,
            String module,
            String action
    ) {
        Page<OperationLog> result = operationLogMapper.selectPage(
                new Page<>(Math.max(page, 1), Math.min(Math.max(pageSize, 1), 100)),
                Wrappers.<OperationLog>lambdaQuery()
                        .eq(StringUtils.hasText(module), OperationLog::getModule, module)
                        .eq(StringUtils.hasText(action), OperationLog::getAction, action)
                        .orderByDesc(OperationLog::getCreatedAt)
        );
        return new PageResult<>(result.getTotal(), result.getRecords());
    }
}
