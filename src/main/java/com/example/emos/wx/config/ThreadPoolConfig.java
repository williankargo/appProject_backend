package com.example.emos.wx.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class ThreadPoolConfig {


    @Bean("AsyncTaskExecutor")
    public AsyncTaskExecutor taskExecutor(){ // spring的多線程，AsyncTaskExecutor是ThreadPoolTaskExecutor的父類

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 設置核心線程數
        executor.setCorePoolSize(8);
        // 設置最大線程數
        executor.setMaxPoolSize(16);
        // 設置線程隊列容量
        executor.setQueueCapacity(32);
        // 設置線程活躍時間（秒）
        executor.setKeepAliveSeconds(60);
        // 設置默認線程名稱
        executor.setThreadNamePrefix("task-");
        // 設置拒絕策略
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        executor.initialize();
        return executor;
    }

}
