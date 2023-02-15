package com.atguigu.gulimall.member.service;

import com.atguigu.gulimall.member.exception.PhoneExistExcecption;
import com.atguigu.gulimall.member.exception.UsernameExistExcecption;
import com.atguigu.gulimall.member.vo.AccessTokenEntity;
import com.atguigu.gulimall.member.vo.MemberLoginVo;
import com.atguigu.gulimall.member.vo.MemberRegistVo;
import com.atguigu.gulimall.member.vo.SocialUser;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimall.member.entity.MemberEntity;

import java.io.IOException;
import java.util.Map;

/**
 * 会员
 *
 * @author chen
 * @email sunlightcs@gmail.com
 * @date 2022-09-20 13:12:28
 */
public interface MemberService extends IService<MemberEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void regist(MemberRegistVo vo);

    /**
     * 检测手机号是不是唯一
     * @param phone
     * @return
     */
     void checkPhoneUnique(String phone) throws PhoneExistExcecption;

    /**
     * 检测用户名是不是唯一
     * @param userName
     * @return
     */
    void checkUserNameUnique(String userName)throws UsernameExistExcecption;


    MemberEntity login(MemberLoginVo vo);

    MemberEntity login(SocialUser socialUser) throws IOException;
}

