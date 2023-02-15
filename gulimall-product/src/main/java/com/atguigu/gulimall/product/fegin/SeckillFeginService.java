package com.atguigu.gulimall.product.fegin;

import com.atguigu.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient("gulimall-seckill")
public interface SeckillFeginService {
    @GetMapping("/sku/seckill/{skuId}")
     R getskuSeckillInfo(@PathVariable("skuId") Long skuId);
}
