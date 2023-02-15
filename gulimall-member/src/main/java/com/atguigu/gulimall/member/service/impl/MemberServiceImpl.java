package com.atguigu.gulimall.member.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.gulimall.member.dao.MemberLevelDao;
import com.atguigu.gulimall.member.entity.MemberLevelEntity;
import com.atguigu.gulimall.member.exception.PhoneExistExcecption;
import com.atguigu.gulimall.member.exception.UsernameExistExcecption;
import com.atguigu.gulimall.member.vo.AccessTokenEntity;
import com.atguigu.gulimall.member.vo.MemberLoginVo;
import com.atguigu.gulimall.member.vo.MemberRegistVo;
import com.atguigu.gulimall.member.vo.SocialUser;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.member.dao.MemberDao;
import com.atguigu.gulimall.member.entity.MemberEntity;
import com.atguigu.gulimall.member.service.MemberService;


@Service("memberService")
public class MemberServiceImpl extends ServiceImpl<MemberDao, MemberEntity> implements MemberService {
    @Autowired
    private MemberLevelDao memberLevelDao;
    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<MemberEntity> page = this.page(
                new Query<MemberEntity>().getPage(params),
                new QueryWrapper<MemberEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public void regist(MemberRegistVo vo) {
        MemberEntity entity = new MemberEntity();
        //1.设置默认等级
       MemberLevelEntity levelEntity =memberLevelDao.getDefaultLevel();
       entity.setLevelId(levelEntity.getId());
       
       //2.检测用户名和手机号是否唯一.为了让controller能感知异常,异常机制
        checkPhoneUnique(vo.getPhone());
        checkUserNameUnique(vo.getUserName());
        
       entity.setMobile(vo.getPhone());
       entity.setUsername(vo.getUserName());
       entity.setNickname(vo.getUserName());
       //密码要进行加密存储
        BCryptPasswordEncoder passwordEncoder=new BCryptPasswordEncoder();
        String encode = passwordEncoder.encode(vo.getPassword());
        entity.setPassword(encode);
        //其它的默认信息
        
        //保持用户
        baseMapper.insert(entity);
    }

    @Override
    public void checkPhoneUnique(String phone) throws PhoneExistExcecption {
        Integer count = baseMapper.selectCount(new QueryWrapper<MemberEntity>().eq("mobile", phone));
        if(count>0){
            throw  new PhoneExistExcecption();
        }
    }


    @Override
    public void checkUserNameUnique(String userName)throws UsernameExistExcecption {
        Integer count = baseMapper.selectCount(new QueryWrapper<MemberEntity>().eq("username", userName));
        if(count>0){
            throw new UsernameExistExcecption();
        }
 
    }

    @Override
    public MemberEntity login(MemberLoginVo vo) {
        String loginacct = vo.getLoginacct();
        String password = vo.getPassword();
        
        //1.去数据库查询
        //SELECT * FROM  ums_member WHERE  username=? OR mobile=?
        MemberDao memberDao=this.baseMapper;
        MemberEntity memberEntity = memberDao.selectOne(new QueryWrapper<MemberEntity>().eq("username", loginacct).or().eq("mobile", loginacct));
        
        if(memberEntity==null){
            //登录失败
            return null;
        }else {
            //1.获取到数据库的password
            String passwordDB = memberEntity.getPassword();
            BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
            //2.密码匹配
            boolean matches = passwordEncoder.matches(password, passwordDB);
            if(matches){
                return memberEntity;
            }else {
                return null;
            }
        }
    }

    @Override
    public MemberEntity login(SocialUser socialUser) throws IOException {
        //具有登录和注册合并逻辑
        //1.判断当前社交用户是否已经登录过系统
        MemberDao memberDao=this.baseMapper;
        MemberEntity memberEntity = memberDao.selectOne(new QueryWrapper<MemberEntity>().eq("expires_in", socialUser.getExpires_in()));
        if(memberEntity!=null){
            //这个用户已经注册
            MemberEntity update=new MemberEntity();
            update.setId(memberEntity.getId());
            update.setAccessToken(socialUser.getAccess_token());
            update.setExpiresIn(socialUser.getExpires_in());
            memberDao.updateById(update);
            memberEntity.setAccessToken(socialUser.getAccess_token());
            memberEntity.setExpiresIn(socialUser.getExpires_in());
            return memberEntity;
        }else {
            //2.没有查到当前社交用户对应的记录我们需要注册一个
            MemberEntity regist=new MemberEntity();
            try {
                HttpResponse response = getHttpResponse(socialUser);
                if(response.getStatusLine().getStatusCode()==200){
                    //4.获取结果
                    String result = EntityUtils.toString(response.getEntity());
                    JSONObject jsonObject = JSON.parseObject(result);
                    String name1 =jsonObject.getString("name");
                    long UserId = Long.parseLong(jsonObject.getString("id"));
                    regist.setNickname(name1);
                    regist.setCreatedAt(String.valueOf(UserId));
                }
            }catch (Exception e){
                
            }
            regist.setCreatedAt(socialUser.getCreated_at());
            regist.setAccessToken(socialUser.getAccess_token());
            regist.setExpiresIn(socialUser.getExpires_in());
            memberDao.insert(regist);
            return regist;
        }
    }

    private static HttpResponse getHttpResponse(SocialUser socialUser) throws IOException {
        HttpClient client = HttpClients.createDefault();
        String access_token = socialUser.getAccess_token();
        HttpGet httpGet = new HttpGet("https://gitee.com/api/v5/user?access_token="+access_token+"");
        HttpResponse response = client.execute(httpGet);
        return response;
    }
}