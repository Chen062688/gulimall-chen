package com.atguigu.gulimall.member.exception;

public class PhoneExistExcecption  extends RuntimeException{
    public PhoneExistExcecption() {
        super("手机号存在");
    }
}
