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
        //1.??????????????????
       MemberLevelEntity levelEntity =memberLevelDao.getDefaultLevel();
       entity.setLevelId(levelEntity.getId());
       
       //2.???????????????????????????????????????.?????????controller???????????????,????????????
        checkPhoneUnique(vo.getPhone());
        checkUserNameUnique(vo.getUserName());
        
       entity.setMobile(vo.getPhone());
       entity.setUsername(vo.getUserName());
       entity.setNickname(vo.getUserName());
       //???????????????????????????
        BCryptPasswordEncoder passwordEncoder=new BCryptPasswordEncoder();
        String encode = passwordEncoder.encode(vo.getPassword());
        entity.setPassword(encode);
        //?????????????????????
        
        //????????????
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
        
        //1.??????????????????
        //SELECT * FROM  ums_member WHERE  username=? OR mobile=?
        MemberDao memberDao=this.baseMapper;
        MemberEntity memberEntity = memberDao.selectOne(new QueryWrapper<MemberEntity>().eq("username", loginacct).or().eq("mobile", loginacct));
        
        if(memberEntity==null){
            //????????????
            return null;
        }else {
            //1.?????????????????????password
            String passwordDB = memberEntity.getPassword();
            BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
            //2.????????????
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
        //?????????????????????????????????
        //1.???????????????????????????????????????????????????
        MemberDao memberDao=this.baseMapper;
        MemberEntity memberEntity = memberDao.selectOne(new QueryWrapper<MemberEntity>().eq("expires_in", socialUser.getExpires_in()));
        if(memberEntity!=null){
            //????????????????????????
            MemberEntity update=new MemberEntity();
            update.setId(memberEntity.getId());
            update.setAccessToken(socialUser.getAccess_token());
            update.setExpiresIn(socialUser.getExpires_in());
            memberDao.updateById(update);
            memberEntity.setAccessToken(socialUser.getAccess_token());
            memberEntity.setExpiresIn(socialUser.getExpires_in());
            return memberEntity;
        }else {
            //2.?????????????????????????????????????????????????????????????????????
            MemberEntity regist=new MemberEntity();
            try {
                HttpResponse response = getHttpResponse(socialUser);
                if(response.getStatusLine().getStatusCode()==200){
                    //4.????????????
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