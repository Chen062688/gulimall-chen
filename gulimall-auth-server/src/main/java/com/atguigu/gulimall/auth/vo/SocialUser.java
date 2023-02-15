/**
 * Copyright 2022 json.cn 
 */
package com.atguigu.gulimall.auth.vo;

import lombok.Data;

/**
 * Auto-generated: 2022-10-04 15:20:59
 *
 * @author json.cn (i@json.cn)
 * @website http://www.json.cn/java2pojo/
 */
@Data
public class SocialUser {
    private String access_token;
    private String token_type;
    private long expires_in;
    private String refresh_token;
    private String scope;
    private long created_at;
}