package com.atguigu.gulimall.order.fegin;

import com.atguigu.gulimall.order.vo.OrderItemVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient("gulimall-cart")
public interface CartFeginService {

    @GetMapping("/currentUserCartItems")
     List<OrderItemVo> currentUserCartItems();
}
