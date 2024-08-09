package com.luluroute.ms.routerules.business.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableAsync
public class SpringAsyncConfig {

    @Value("${config.async.createshipment.corepoolsize}")
    private int createCorePoolSize;

    @Value("${config.async.createshipment.maxpoolsize}")
    private int createMaxPoolSize;

    @Bean(name = "CreateShipmentTaskExecutor")
    public Executor createShipmentTaskExecutor() {
        return new ThreadPoolExecutor(createCorePoolSize, createMaxPoolSize, 30, TimeUnit.SECONDS,
                new SynchronousQueue<>(),
                new ThreadPoolExecutor.CallerRunsPolicy());

    }
}
