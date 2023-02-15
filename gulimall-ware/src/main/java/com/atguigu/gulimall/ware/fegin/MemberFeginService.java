package com.atguigu.gulimall.ware.fegin;

import com.atguigu.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@FeignClient("gulimall-member")
public interface MemberFeginService {
    @RequestMapping("/member/memberreceiveaddress/info/{id}")
     R addrInfo(@PathVariable("id") Long id);
}
