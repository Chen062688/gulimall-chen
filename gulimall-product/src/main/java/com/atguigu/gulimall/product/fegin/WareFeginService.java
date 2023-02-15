package com.atguigu.gulimall.product.fegin;

import com.atguigu.common.to.SkuHasStockVo;
import com.atguigu.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient("gulimall-ware")
public interface WareFeginService {

    /**
     * 1.R设计的时候可以加上泛型
     * 2.直接返回我们想要的结果
     * @param skuIds
     * @return
     */
    @PostMapping("/ware/waresku/hasStock") 
    R  getSkusHasStock(@RequestBody List<Long> skuIds);
}
