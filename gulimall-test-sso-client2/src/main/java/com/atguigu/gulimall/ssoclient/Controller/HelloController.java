package com.atguigu.gulimall.ssoclient.Controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;

@Controller
public class HelloController {
    
    @Value("${sso.server.url}")
    String ssoServerUrl;
    
    
    
    /**
     * 无需登录就可以访问
     * @return
     */
    @GetMapping("/hello")
    @ResponseBody
    public String hello(){
        
        
        return "hello";
    }

    /**
     * 感知这次是在ssoserver 登录成功跳回来的。
     * @param model
     * @param session
     * @param token  只要去ssoserver 登录成功跳回来就会带上
     * @return
     */
    @GetMapping("/boss")
    public String employees(Model model, HttpSession session,@RequestParam(value = "token",required = false)String token){
        if(!StringUtils.isEmpty(token)){
            //去ssoserver 登录成功跳回来就会带上
            //TODO 1.去ssoserver获取当前token对应真正的用户信息
            RestTemplate restTemplate=new RestTemplate();
            ResponseEntity<String> response = restTemplate.getForEntity("http://ssoserver.com:8080/userInfo?token=" + token, String.class);
            String body = response.getBody();
            session.setAttribute("loginUser",body);
        }
        Object loginUser = session.getAttribute("loginUser");
        if(loginUser==null){
            //没登录,跳转到登录服务器进行登录
            
            return "redirect:"+ssoServerUrl+"?redirect_url=http://client2.com:8082/boss";
        }else {
            
        }
        List<String> emps=new ArrayList<>();
        emps.add("张三");
        emps.add("李四");
        emps.add("王五");
        model.addAttribute("emps",emps);
        return "list";
    }
}
