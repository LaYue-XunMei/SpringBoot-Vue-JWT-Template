package com.example.utils;

import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class FlowUtils {

    @Resource
    private RedisTemplate<String, String> redisTemplate;  // 改用 RedisTemplate<String, String>

    public boolean limitOnceChick(String key, int blockTime){//redis限流
        if(Boolean.TRUE.equals(redisTemplate.hasKey(key))){//如果正在冷却时间内
            return false;//现在不能请求
        }else {//如果用户没有被封禁，丢一个封禁的键到redis，标志限制block秒，不能验证
            redisTemplate.opsForValue()
                    .set(key,"",blockTime, TimeUnit.SECONDS);//设置key的值为空，blockTime秒后过期
            return true;
        }
    }

}
