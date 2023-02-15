package com.atguigu.gulimall.product.config;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.PaginationInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@MapperScan("com.atguigu.gulimall.product.dao")
public class MybatisConfig {
    //引入分页插件
    @Bean
    public PaginationInterceptor paginationInterceptor() {
      PaginationInterceptor paginationInterceptor=new PaginationInterceptor();
        
      paginationInterceptor.setOverflow(true);
      
      paginationInterceptor.setLimit(1000);
      return paginationInterceptor;
    }
}
