package com.atguigu.gulimall.seckill.controllera;

import com.atguigu.gulimall.seckill.service.SeckillService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AAA {
    @Autowired
    SeckillService seckillService;
    
    @GetMapping("/kill")
    public String seckill(@RequestParam("killId") String killId, @RequestParam("key") String key,
                          @RequestParam("num") Integer num, Model model){
        //1.判断是否登录
        String orderSn =seckillService.kull(killId,key,num);
        model.addAttribute("orderSn",orderSn);
        return "success";
    }
}
