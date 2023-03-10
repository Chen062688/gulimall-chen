package com.atguigu.gulimall.order.fegin;

import com.atguigu.gulimall.order.vo.MemberAddressVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient("gulimall-member")
public interface MemberFeginService {
    @GetMapping("/member/memberreceiveaddress/{memberId}/address")
     List<MemberAddressVo> getAddress(@PathVariable("memberId") Long memberId);
}
