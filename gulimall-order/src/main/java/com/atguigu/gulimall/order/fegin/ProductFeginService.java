package com.atguigu.gulimall.order.fegin;

import com.atguigu.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
@FeignClient("gulimall-product")
public interface ProductFeginService {

    @GetMapping("/product/spuinfo/skuId/{id}")
     R getspuInfoBySkuId(@PathVariable("id") Long skuId);
}
