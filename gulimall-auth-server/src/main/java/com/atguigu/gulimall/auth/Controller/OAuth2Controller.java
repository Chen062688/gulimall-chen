package com.atguigu.gulimall.auth.Controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.auth.fegin.MemberFefinService;
import com.atguigu.common.vo.MemberRespVo;
import com.atguigu.gulimall.auth.vo.SocialUser;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * 处理社交登录请求
 */
@Controller
@Slf4j
public class OAuth2Controller {
    @Autowired
    MemberFefinService memberFefinService;

    /**
     * 社交登录成功回调
     * @param code
     * @return
     * @throws IOException
     */
    @GetMapping("/oauth/gitee/success")
    public String getAccessToken(@RequestParam("code")  String code, HttpSession session) throws IOException {

        String ClientId = "c60cdd719de8f1f04bdb34efc4c436d7533b3ab0d36216f0d6beb7007cabb145";
        String ClientSecret = "8609337be02a937ac6ce832f402b4d082ba6f58754f923bd73651bbde23f32e7";
        String RedirectUrl = "http://auth.gulimall.com/oauth/gitee/success";

        // 根据 code 来获取access token
        String url = "https://gitee.com/oauth/token?grant_type=authorization_code" +
                "&client_id=" + ClientId +
                "&client_secret=" + ClientSecret +
                "&code=" + code +
                "&redirect_uri=" + RedirectUrl;

        // 使用HttpClient 发送post请求
        HttpClient client = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(url);
        HttpResponse response = client.execute(httpPost);
        if(response.getStatusLine().getStatusCode()==200){
            // 获取Json格式响应体内容, 响应体里就有我们需要的accessToken
            HttpEntity entity = response.getEntity();
            String jsonString = EntityUtils.toString(entity, "UTF-8");
            // 使用工具将Json格式字符串封装成对象
            SocialUser socialUser = JSON.parseObject(jsonString, SocialUser.class);
            //知道当前是哪个社交用户
            System.out.println(socialUser.toString());
            //1)当前用户如果是第一次进这个网站,就自动注册进来(为当前社交用户生成一个会员信息账户,以后这个社交账户,就对应指定的会员)
            //登录或者注册这个社交用户
            //TODO 1.默认发的令牌。 session=dawdwa。作用域:是当前域 (解决子域session共享问题)
            //TODO 2.使用JSON序列化方式来序列化对象数据到redis
            R r = memberFefinService.oauth2login(socialUser);
            if(r.getCode()==0){
                MemberRespVo data = r.getData("data", new TypeReference<MemberRespVo>() {
                });
                log.info("登录成功:用户信息{}"+data.toString());
                session.setAttribute("loginUser",data);
                return "redirect:http://gulimall.com";
            }else {
                return "redirect:http://auth.gulimall.com/login.html";
            }
        }else {
            return "redirect:http://auth.gulimall.com/login.html";
        }
    }
}
