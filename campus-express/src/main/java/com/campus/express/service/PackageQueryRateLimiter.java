package com.campus.express.service;

import com.campus.express.common.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 学生查件内存限流器
 */
@Component
public class PackageQueryRateLimiter {

    private static final long WINDOW_SECONDS = 60;
    private static final int MAX_REQUESTS = 10;

    private final ConcurrentHashMap<String, Deque<Long>> requests = new ConcurrentHashMap<>();

    public void check(String scope, String subject) {
        long now = Instant.now().getEpochSecond();
        String key = scope + ":" + subject;
        Deque<Long> timestamps = requests.computeIfAbsent(key, ignored -> new ArrayDeque<>());

        synchronized (timestamps) {
            while (!timestamps.isEmpty() && timestamps.peekFirst() <= now - WINDOW_SECONDS) {
                timestamps.removeFirst();
            }
            if (timestamps.size() >= MAX_REQUESTS) {
                throw new BusinessException(429, "查询过于频繁，请稍后再试");
            }
            timestamps.addLast(now);
        }
    }
}
