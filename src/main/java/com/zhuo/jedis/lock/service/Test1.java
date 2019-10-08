package com.zhuo.jedis.lock.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class Test1 {
    @Autowired
    private StringRedisTemplate redisTemplate;

    public void test() {
        RedisLock redisLock = new RedisLock("hhh", redisTemplate);
        new Thread(() -> {
//            redisLock.lock();
//            System.out.println("线程1锁了");
//            try {
//                Thread.sleep(10000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            System.out.println("线程1解锁了");
            redisLock.unlock();

        }).start();
        new Thread(() -> {
            redisLock.lock();
            System.out.println("线程2锁了");
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            redisLock.unlock();
            System.out.println("线程2解锁了");
        }).start();
    }
}
