package com.atguigu.gulimall.search.fegin;

import com.atguigu.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient("gulimall-product")
public interface ProductFeginService {
    
    @GetMapping("/product/attr/info/{attrId}")
    //@RequiresPermissions("product:attr:info")
     R attrInfo(@PathVariable("attrId") Long attrId);

    @GetMapping("/product/brand/infos")
     R brandsInfo(@RequestParam("brandIds") List<Long> brandIds);
}
