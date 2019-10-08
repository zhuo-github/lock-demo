package com.zhuo.jedis.lock.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class RedisLock implements Lock {

    private static final String REDISS_LOCK_PREFIX = "redis::lock::";
    private StringRedisTemplate template;
    private String keyName;

    RedisLock(String keyName, StringRedisTemplate template) {
        this.keyName = keyName;
        this.template = template;
    }

    @Override
    public void lock() {
        lock(-1, null);
    }

    public void lock(long time, TimeUnit unit) {
        boolean tryLock = false;
        while (!tryLock) {
            tryLock = tryLock(time, unit);
        }
    }

    @Override
    public void lockInterruptibly() {
        lock(-1, null);
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
        Boolean result = template.opsForValue()
                                 .setIfAbsent(REDISS_LOCK_PREFIX + keyName, threadId, time, unit);
        return result == null ? false : result;
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
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        DefaultRedisScript<Boolean> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(script);
        redisScript.setResultType(Boolean.class);
        Boolean result = template.execute(redisScript, Collections.singletonList(REDISS_LOCK_PREFIX + keyName),
                                          threadId);
        System.out.println(result);
    }

    @Override
    public Condition newCondition() {
        return null;
    }
}
