package com.atguigu.gulimall.seckill.fegin;

import com.atguigu.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@FeignClient("gulimall-product")
public interface ProductFeginService {

    @RequestMapping("/product/skuinfo/info/{skuId}")
     R SkuInfo(@PathVariable("skuId") Long skuId);
}
