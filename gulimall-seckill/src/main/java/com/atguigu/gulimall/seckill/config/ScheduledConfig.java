package com.atguigu.gulimall.seckill.config;


import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling//开启定时调度
@Configuration
@EnableAsync//开启异步
public class ScheduledConfig {
}
