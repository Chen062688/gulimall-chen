package com.atguigu.common.exception;

public enum BizCodeEnume {
    UNKNOW_EXCEPTION(10000,"系统未知异常"),

    VAILD_EXCEPTION(10001,"参数格式校验失败"),
    SMS_CODE_EXCEPTION(10002,"验证码获取频率太高,稍后再试"),
    USER_EXIST_EXCEPTION(15001,"用户名存在异常"),
    PHONE_EXIST_EXCEPTION(15002,"手机号存在异常"),
    LOGINACCT_PASSWORD_INVAILD_EXCEPTION(15003,"账户密码错误"),
    NO_STOCK_EXCEPTION(21000,"商品库存不足"),
    PRODUCT_UP_EXCEPTION(11000,"商品上架异常");

    private int code;
    private String msg;

    BizCodeEnume(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}
