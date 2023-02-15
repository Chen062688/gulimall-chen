package com.atguigu.gulimall.member.exception;

public class UsernameExistExcecption extends RuntimeException {
    public UsernameExistExcecption() {
        super("用户名存在");
    }
}
