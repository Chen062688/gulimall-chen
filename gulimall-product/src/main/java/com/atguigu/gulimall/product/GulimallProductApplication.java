package com.atguigu.gulimall.product;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * @Cacheable 缓存方法返回值
 * @CacheConfig 类级别注解用于抽取当前类下的缓存配置公共属性例如cacheNames，方法上有相同属性时，方法优先级高
 * @CacheEvict 用于删除缓存数据
 * @CachePut 用于更新缓存操作，始终会执行方法逻辑，感觉此注解比较鸡肋，用它就需要注意方法写法返回值必须和缓存的方法返回值一致
 * @Caching 复杂缓存的实现，比如多维度key，当然也可以拆成简单的缓存
 *
 */
@EnableRedisHttpSession
@EnableFeignClients(basePackages = "com.atguigu.gulimall.product.fegin")
@SpringBootApplication
@MapperScan("com.atguigu.gulimall.product.dao")
@EnableDiscoveryClient
public class GulimallProductApplication {

    public static void main(String[] args) {
        SpringApplication.run(GulimallProductApplication.class, args);
    }

}
