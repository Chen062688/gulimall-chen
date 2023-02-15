package com.atguigu.gulimall.seckill.fegin;

import com.atguigu.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient("gulimall-coupon")
public interface CouponFeginService {
    @GetMapping("/coupon/seckillsession/Lates3DaySession")
     R getLates3DaySession();
}
