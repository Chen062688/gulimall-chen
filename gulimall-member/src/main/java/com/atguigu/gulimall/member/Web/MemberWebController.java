package com.atguigu.gulimall.member.Web;

import com.alibaba.fastjson.JSON;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.member.fegin.OrderFeginService;
import org.apache.http.HttpRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Controller
public class MemberWebController {
    
    @Autowired
    OrderFeginService orderFeginService;
    @GetMapping("/memberOrder.html")
    public String memberOrder(@RequestParam(value = "pageNum",defaultValue = "1") Integer pageNum,
                              Model model, HttpServletRequest request){
        //获取到支付宝给我们传来的所以请求数据;
        //验证签名 如果正确可以去修改。
        
        //查出当前登录的用户的所有订单列表数据
        Map<String,Object>page=new HashMap<>();
        page.put("page",pageNum.toString());
        R r = orderFeginService.listWithItem(page);
        System.out.println(JSON.toJSONString(r));
        model.addAttribute("orders",r);
        return "orderList";
    }
}
