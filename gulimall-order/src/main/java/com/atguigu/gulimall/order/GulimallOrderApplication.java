package com.atguigu.gulimall.order;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * 使用Rabbitmq
 * 1.引入amqp场景
 * 2.给容器中自动配置了 
 * RabbitTemplate,AmqpAdmin、
 * 
 * exposeProxy = true 对外暴露代理对象
 * 
 * 本类互调用调用对象
 * 
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableRabbit
@EnableRedisHttpSession
@EnableFeignClients
@EnableAspectJAutoProxy(exposeProxy = true) //开启aspectj动态代理功能。以后所有的动态代理都是aspectj创建的.(即使没有接口也能可以创建动态代理)
public class GulimallOrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(GulimallOrderApplication.class, args);
    }

}
