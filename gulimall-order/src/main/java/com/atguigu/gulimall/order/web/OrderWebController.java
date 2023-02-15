package com.atguigu.gulimall.order.web;

import com.atguigu.gulimall.order.service.OrderService;
import com.atguigu.gulimall.order.vo.OrderConfirmVo;
import com.atguigu.gulimall.order.vo.OrderSubmitVo;
import com.atguigu.gulimall.order.vo.SubmitOrderResponseVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.ExecutionException;

@Controller
public class OrderWebController {
    @Autowired
    private OrderService orderService;
        
    @GetMapping("/toTrade")
    public String toTrade(Model model, HttpServletRequest request) throws ExecutionException, InterruptedException {
     OrderConfirmVo confirmVo=orderService.confirmOrder();
     model.addAttribute("orderConfirmData",confirmVo);
        //展示订单确认的数据
        return "confirm";
    }

    /**
     * 下单功能
     * @param vo
     * @return
     */
    @PostMapping("/submitOrder")
    public String submitOrder(OrderSubmitVo vo, Model model, RedirectAttributes redirectAttributes){
       SubmitOrderResponseVo responseVo = orderService.submitOrder(vo); 
       if(responseVo.getCode()==0){
           //下单成功:来到支付选择页 
           model.addAttribute("submitOrderResp",responseVo);
           return "pay";
       }else {
           //下单失败回到订单确认页重新确认订单信息
           String msg="下单失败!";
           switch (responseVo.getCode()){
               case 1: msg+="订单信息过期,请刷新再次提交";break;
               case 2: msg+="订单商品价格发送变化,请确认后再次提交";break;
               case 3:  msg+="锁库锁定失败了,商品库存不足"; break;
           }
           redirectAttributes.addFlashAttribute("msg",msg);
        return "redirect:http://order.gulimall.com/toTrade";
       }
    }
}
