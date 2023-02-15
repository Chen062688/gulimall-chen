package com.atguigu.gulimall.cart.fegin;

import com.atguigu.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.util.List;

@FeignClient("gulimall-product")
public interface ProductFeginService {
 
    @RequestMapping("product/skuinfo/info/{skuId}")
    //@RequiresPermissions("product:spuinfo:info")
     R getSkuInfo(@PathVariable("skuId") Long skuId);

    @GetMapping("/product/skusaleattrvalue/stringlist/{skuId}")
     List<String> getSkuSaleAttrValues(@PathVariable("skuId") Long skuId);

    /**
     * 查询最新价格
     */
    @GetMapping("/product/skuinfo/{skuId}/price")
     R getPrice(@PathVariable("skuId") Long skuId);    
}
