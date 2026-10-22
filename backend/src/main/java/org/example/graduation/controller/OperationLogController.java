package org.example.graduation.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.example.graduation.common.Result;
import org.example.graduation.entity.SysOperationLog;
import org.example.graduation.mapper.SysOperationLogMapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/operation-logs")
@RequiredArgsConstructor
public class OperationLogController {

    private final SysOperationLogMapper sysOperationLogMapper;

    @GetMapping
    @PreAuthorize("hasAuthority('log:read')")
    public Result<?> list(@RequestParam(defaultValue = "1") Integer page,
                          @RequestParam(defaultValue = "10") Integer size,
                          @RequestParam(required = false) String username,
                          @RequestParam(required = false) String operation,
                          @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<SysOperationLog> wrapper = new LambdaQueryWrapper<SysOperationLog>()
                .eq(username != null && !username.isEmpty(), SysOperationLog::getUsername, username)
                .like(operation != null && !operation.isEmpty(), SysOperationLog::getOperation, operation)
                .eq(status != null, SysOperationLog::getStatus, status)
                .orderByDesc(SysOperationLog::getCreatedAt);

        Page<SysOperationLog> result = sysOperationLogMapper.selectPage(new Page<>(page, size), wrapper);
        return Result.success(result);
    }
}