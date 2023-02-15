package com.atguigu.gulimall.member.dao;

import com.atguigu.gulimall.member.entity.MemberEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员
 * 
 * @author chen
 * @email sunlightcs@gmail.com
 * @date 2022-09-20 13:12:28
 */
@Mapper
public interface MemberDao extends BaseMapper<MemberEntity> {
	
}
