package org.example.graduation.security;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录失败次数限制。
 *
 * 以“用户名 + 客户端 IP”为维度，在时间窗口内累计失败次数；
 * 达到上限后锁定一段时间，防止暴力破解。
 * 当前为单机内存实现，重启后计数清空，适用于本项目的单体部署场景。
 */
@Component
public class LoginAttemptService {

    /** 时间窗口内允许的最大失败次数 */
    private static final int MAX_ATTEMPTS = 5;

    /** 失败计数时间窗口（15 分钟） */
    private static final long WINDOW_MILLIS = 15 * 60 * 1000L;

    /** 达到上限后的锁定时长（15 分钟） */
    private static final long LOCK_MILLIS = 15 * 60 * 1000L;

    /** 记录上限，防止大量随机用户名导致内存无限增长 */
    private static final int MAX_ENTRIES = 10000;

    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();

    /**
     * 原子地占用一次登录尝试，并判断是否仍允许继续验证密码。
     *
     * 计数在密码校验之前完成，避免并发请求同时通过锁定期检查。
     *
     * @return true 表示允许本次尝试；false 表示已锁定
     */
    public boolean tryAcquire(String key) {
        if (attempts.size() >= MAX_ENTRIES && !attempts.containsKey(key)) {
            evictOne();
        }

        long now = System.currentTimeMillis();
        Attempt attempt = attempts.compute(key, (k, existing) -> {
            if (existing == null || isExpired(existing, now)) {
                existing = new Attempt();
                existing.windowStart = now;
            }
            existing.count++;
            if (existing.count == MAX_ATTEMPTS + 1) {
                existing.lockedUntil = now + LOCK_MILLIS;
            }
            return existing;
        });

        synchronized (attempt) {
            return attempt.lockedUntil <= now;
        }
    }

    /**
     * 登录成功，清除该账号/IP 的失败记录。
     */
    public void loginSucceeded(String key) {
        attempts.remove(key);
    }

    /**
     * 锁定期内不过期；锁定结束后视为过期并重置窗口；
     * 未锁定则按时间窗口判断。
     */
    private boolean isExpired(Attempt attempt, long now) {
        if (attempt.lockedUntil > now) {
            return false;
        }
        if (attempt.lockedUntil > 0) {
            return true;
        }
        return now - attempt.windowStart > WINDOW_MILLIS;
    }

    /**
     * 达到容量上限时淘汰一个记录，保证 Map 大小有硬上限。
     */
    private void evictOne() {
        var iterator = attempts.keySet().iterator();
        if (iterator.hasNext()) {
            iterator.next();
            iterator.remove();
        }
    }

    private static class Attempt {
        private int count;
        private long windowStart;
        private long lockedUntil;
    }
}