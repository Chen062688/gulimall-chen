package com.atguigu.gulimall.member.fegin;


import com.atguigu.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient("gulimall-order")
public interface OrderFeginService {

    @PostMapping("/order/order/listWithItem")
     R listWithItem(@RequestBody Map<String, Object> params);
}
