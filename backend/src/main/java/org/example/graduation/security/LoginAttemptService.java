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

    /** 超过该数量时清理过期记录，避免内存无限增长 */
    private static final int CLEANUP_THRESHOLD = 10000;

    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();

    /**
     * 判断该账号/IP 当前是否处于锁定期。
     */
    public boolean isBlocked(String key) {
        Attempt attempt = attempts.get(key);
        if (attempt == null) {
            return false;
        }
        synchronized (attempt) {
            long now = System.currentTimeMillis();
            if (attempt.lockedUntil > now) {
                return true;
            }
            if (isExpired(attempt, now)) {
                attempts.remove(key, attempt);
            }
            return false;
        }
    }

    /**
     * 记录一次登录失败；达到阈值后自动锁定。
     */
    public void loginFailed(String key) {
        if (attempts.size() > CLEANUP_THRESHOLD) {
            evictExpired();
        }
        attempts.compute(key, (k, attempt) -> {
            long now = System.currentTimeMillis();
            if (attempt == null || isExpired(attempt, now)) {
                attempt = new Attempt();
                attempt.windowStart = now;
            }
            attempt.count++;
            if (attempt.count >= MAX_ATTEMPTS) {
                attempt.lockedUntil = now + LOCK_MILLIS;
            }
            return attempt;
        });
    }

    /**
     * 登录成功，清除该账号/IP 的失败记录。
     */
    public void loginSucceeded(String key) {
        attempts.remove(key);
    }

    private boolean isExpired(Attempt attempt, long now) {
        return attempt.lockedUntil <= now && now - attempt.windowStart > WINDOW_MILLIS;
    }

    private void evictExpired() {
        long now = System.currentTimeMillis();
        attempts.entrySet().removeIf(entry -> isExpired(entry.getValue(), now));
    }

    private static class Attempt {
        private int count;
        private long windowStart;
        private long lockedUntil;
    }
}