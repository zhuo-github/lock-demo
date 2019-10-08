package com.zhuo.jedis.lock.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

@Slf4j
public class RedisLock implements Lock {

    private static final String REDIS_LOCK_PREFIX = "redis::lock::";
    private StringRedisTemplate template;
    private String keyName;

    RedisLock(String keyName, StringRedisTemplate template) {
        this.keyName = keyName;
        this.template = template;
    }

    /**
     * 加cas不可中断锁，且锁不超时
     */
    @Override
    public void lock() {
        lock(-1, null, -1);
    }

    /**
     * @param time 锁超时时间，-1为不超时
     * @param unit 超时时间单位
     */
    public void lock(long time, TimeUnit unit) {
        lock(time, unit, -1);
    }

    /**
     * @param attempts 尝试获取锁的次数，-1为一直尝试
     */
    public void lock(int attempts) {
        lock(-1, null, attempts);
    }

    /**
     * @param time 锁超时时间，-1为不超时
     * @param unit 超时时间单位
     * @param attempts 尝试获取锁的次数，-1为一直尝试
     */
    public void lock(long time, TimeUnit unit, int attempts) {
        boolean tryLock = false;
        while (!tryLock) {
            tryLock = tryLock(time, unit);
            if (attempts > 0) {
                --attempts;
            } else if (attempts != -1) {
                throw new RuntimeException("超过最大尝试次数");
            }
        }
    }

    @Override
    public void lockInterruptibly() {
        lock(-1, null, -1);
    }

    @Override
    public boolean tryLock() {
        return tryLock(-1, null);
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) {
        String threadId = String.valueOf(Thread.currentThread()
                                               .getId());
        //错误代码，没有原子性
//        Boolean exist = template.hasKey(REDISS_LOCK_PREFIX + keyName);
//        if (exist == null ? false : exist) {
//            String value = template.opsForValue()
//                                   .get(REDISS_LOCK_PREFIX + keyName);
//            return threadId.equals(value);
//        }
        try {
            Boolean result = template.opsForValue()
                                     .setIfAbsent(REDIS_LOCK_PREFIX + keyName, threadId, time, unit);
            return result == null ? false : result;
        } catch (QueryTimeoutException e) {
            return false;
        }
    }

    @Override
    public void unlock() {
        String threadId = String.valueOf(Thread.currentThread()
                                               .getId());
        //错误代码，没有原子性
//        String value = template.opsForValue()
//                               .get(REDISS_LOCK_PREFIX + keyName);
//        if (threadId.equals(value)) {
//            template.delete(REDISS_LOCK_PREFIX + keyName);
//        }
        // 使用lua脚本让操作具有原子性
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        DefaultRedisScript<Boolean> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(script);
        redisScript.setResultType(Boolean.class);
        Boolean result = template.execute(redisScript, Collections.singletonList(REDIS_LOCK_PREFIX + keyName),
                                          threadId);
    }

    /**
     * 强制解锁，没解锁就不断重试
     */
    public void forceUnLock(){
        while (true){
            try {
                unlock();
                break;
            }catch (Exception e){
                log.error(e.getMessage());
            }
        }
    }

    @Override
    public Condition newCondition() {
        return null;
    }
}
