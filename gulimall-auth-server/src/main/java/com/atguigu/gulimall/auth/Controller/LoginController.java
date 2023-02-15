package com.atguigu.gulimall.auth.Controller;

import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.constant.AuthServerConstant;
import com.atguigu.common.exception.BizCodeEnume;
import com.atguigu.common.utils.R;
import com.atguigu.common.vo.MemberRespVo;
import com.atguigu.gulimall.auth.fegin.MemberFefinService;
import com.atguigu.gulimall.auth.fegin.ThirdPartFeginService;
import com.atguigu.gulimall.auth.vo.UserLoginVo;
import com.atguigu.gulimall.auth.vo.UserRegistVo;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Controller
public class LoginController {
    @Autowired
    private ThirdPartFeginService feginService;
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private MemberFefinService memberFefinService;
    @GetMapping("/sms/sendcode")
    @ResponseBody
    public R sendCode(@RequestParam("phone") String phone){
      
        //TODO 1.接口防刷      
        String redisCode = redisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACH_PREFIX + phone);
       if(!StringUtils.isEmpty(redisCode)){
           long l = Long.parseLong(redisCode.split("_")[1]);
           if(System.currentTimeMillis() - l < 60000){
               //60s内不能再发
               return  R.error(BizCodeEnume.SMS_CODE_EXCEPTION.getCode(),BizCodeEnume.SMS_CODE_EXCEPTION.getMsg());
           }
       }
        //2.验证码的再次校验。存入到redis
        String code = UUID.randomUUID().toString().substring(0,4)+"_"+System.currentTimeMillis();
        //redis缓存验证码,防止同一个手机号在60s内再次发送验证码
        redisTemplate.opsForValue().set(AuthServerConstant.SMS_CODE_CACH_PREFIX+phone,code,10, TimeUnit.MINUTES);
        //截取后再发验证码!
        String substring = code.substring(0, 4);
        feginService.sendCode(phone,substring);
        return R.ok();
    }

    /**
     *  //TODO 重定向携带数据,利用session原理.将数据放在session中。
     *  只要跳到下一个页面取出这个数据以后,session里面的数据就会删掉 
     *  //TODO 1.分布式下的session问题
     * @param vo
     * @param result
     * @param model
     * @param redirectAttributes:模拟重定向携带数据
     * @return
     */
    @PostMapping("/regist")
    public String regist(@Valid UserRegistVo vo, BindingResult result, Model model, RedirectAttributes redirectAttributes){
        if(result.hasErrors()){
            Map<String, String> errors = result.getFieldErrors().stream().collect(Collectors.toMap(fieldError -> {
                return fieldError.getField();
            }, fieldError -> {
                return fieldError.getDefaultMessage();
            }));
            redirectAttributes.addFlashAttribute("errors",errors);
            return "redirect:https://auth.gulimall.com/reg.html";
        }
       
        //1.校验验证码
        String code = vo.getCode();
        String s = redisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACH_PREFIX + vo.getPhone());
        if(!StringUtils.isEmpty(s)){
           
            if(code.equals( s.split("_")[0])){
                //删除验证码;令牌机制
                redisTemplate.delete(AuthServerConstant.SMS_CODE_CACH_PREFIX + vo.getPhone());
                //验证码通过。 //真正注册。调用远程服务进行注册
                R r = memberFefinService.regist(vo);
                if(r.getCode()==0){
                    return "redirect:/login.html";
                }else {
                    Map<String,String>errors=new HashMap<>();
                    errors.put("msg",r.getData("msg",new TypeReference<String>(){}));
                    redirectAttributes.addFlashAttribute("errors",errors);
                    return "redirect:http://auth.gulimall.com/reg.html";
                }

            }else {
                Map<String, String> errors = new HashMap<>();
                errors.put("code","验证码错误!");
                redirectAttributes.addFlashAttribute("errors",errors);
                return "redirect:http://auth.gulimall.com/reg.html";
            }
        }else {
            Map<String, String> errors = new HashMap<>();
            errors.put("code","验证码错误!");
            redirectAttributes.addFlashAttribute("errors",errors);
            return "redirect:http://auth.gulimall.com/reg.html";
        }
    }
    @GetMapping("/login.html")
    public String loginPage(HttpSession session){
        Object attribute = session.getAttribute(AuthServerConstant.LOGIN_USER);
        if(attribute==null){
            //没登录展示登录页面
            return "login";
        }else {

            return "redirect:http://gulimall.com";
        }
      
    }
    @PostMapping("/login")
    public String login(UserLoginVo vo, RedirectAttributes redirectAttributes, HttpSession session){
        //远程登录
        R r = memberFefinService.login(vo);
        if(r.getCode()==0){
            //成功!
            MemberRespVo data = r.getData("data", new TypeReference<MemberRespVo>() {
            });
            //成功放到session中
            session.setAttribute(AuthServerConstant.LOGIN_USER,data);
            return "redirect:http://gulimall.com";
        }else {
            //失败
            Map<String,String>errors=new HashMap<>();
            errors.put("msg",r.getData("msg",new TypeReference<String>(){}));
            redirectAttributes.addFlashAttribute("errors",errors);
            return "redirect:http://auth.gulimall.com/login.html";
        }
      
    }
}
