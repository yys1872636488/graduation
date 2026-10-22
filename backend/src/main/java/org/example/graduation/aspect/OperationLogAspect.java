package org.example.graduation.aspect;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.example.graduation.entity.SysOperationLog;
import org.example.graduation.mapper.SysOperationLogMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    /** 日志中必须脱敏的字段名（统一转小写比较） */
    private static final Set<String> SENSITIVE_FIELDS = Set.of(
            "password", "oldpassword", "newpassword", "confirmpassword",
            "token", "secret", "authorization"
    );

    private final SysOperationLogMapper operationLogMapper;
    private final ObjectMapper objectMapper;

    @Around("@annotation(org.springframework.web.bind.annotation.PostMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PutMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.DeleteMapping)")
    public Object aroundWriteOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        SysOperationLog opLog = new SysOperationLog();
        opLog.setStatus(1); // 默认成功

        long start = System.currentTimeMillis();
        try {
            // 获取当前用户
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                opLog.setUsername(auth.getName());
                try {
                    opLog.setUserId(Long.parseLong(auth.getName()));
                } catch (NumberFormatException e) {
                    // principal 可能是用户名而非ID
                }
            }

            // 获取请求信息
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                opLog.setRequestMethod(request.getMethod());
                opLog.setRequestUrl(request.getRequestURI());
                opLog.setRequestIp(getClientIp(request));
            }

            // 记录操作类型
            String methodName = joinPoint.getSignature().getName();
            String className = joinPoint.getSignature().getDeclaringType().getSimpleName();
            opLog.setOperation(className + "." + methodName);

            // 记录参数（跳过文件上传等大对象，并对密码等敏感字段脱敏）
            Object[] args = joinPoint.getArgs();
            if (args != null && args.length > 0 && !hasMultipartArg(args)) {
                try {
                    JsonNode node = objectMapper.valueToTree(args);
                    maskSensitiveFields(node);
                    opLog.setSummary(objectMapper.writeValueAsString(node));
                } catch (Exception e) {
                    opLog.setSummary("[参数序列化失败]");
                }
            }

            Object result = joinPoint.proceed();

            return result;
        } catch (Throwable e) {
            opLog.setStatus(0);
            opLog.setErrorMsg(e.getMessage());
            throw e;
        } finally {
            try {
                operationLogMapper.insert(opLog);
            } catch (Exception e) {
                log.error("保存操作日志失败", e);
            }
        }
    }

    /**
     * 递归遍历 JSON，将密码、令牌等敏感字段替换为 ***，避免明文写入操作日志。
     */
    private void maskSensitiveFields(JsonNode node) {
        if (node == null) {
            return;
        }
        if (node.isArray()) {
            node.forEach(this::maskSensitiveFields);
            return;
        }
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            List<String> sensitiveKeys = new ArrayList<>();
            Iterator<Map.Entry<String, JsonNode>> fields = objectNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                if (SENSITIVE_FIELDS.contains(field.getKey().toLowerCase())) {
                    sensitiveKeys.add(field.getKey());
                } else {
                    maskSensitiveFields(field.getValue());
                }
            }
            sensitiveKeys.forEach(key -> objectNode.put(key, "***"));
        }
    }

    private boolean hasMultipartArg(Object[] args) {
        for (Object arg : args) {
            if (arg != null && arg.getClass().getName().contains("MultipartFile")) {
                return true;
            }
        }
        return false;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip != null && ip.contains(",") ? ip.split(",")[0].trim() : ip;
    }
}