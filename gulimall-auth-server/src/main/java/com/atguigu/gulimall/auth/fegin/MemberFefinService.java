package com.atguigu.gulimall.auth.fegin;

import com.atguigu.common.utils.R;
import com.atguigu.gulimall.auth.vo.SocialUser;
import com.atguigu.gulimall.auth.vo.UserLoginVo;
import com.atguigu.gulimall.auth.vo.UserRegistVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.io.IOException;

@FeignClient("gulimall-member")
public interface MemberFefinService {
    @PostMapping("/member/member/regist")
     R regist(@RequestBody UserRegistVo vo);

    @PostMapping("/member/member/login")
     R login(@RequestBody UserLoginVo vo);
    
    @PostMapping("/member/member/oauth2/login")
     R oauth2login(@RequestBody SocialUser socialUser) throws IOException;
    
}
