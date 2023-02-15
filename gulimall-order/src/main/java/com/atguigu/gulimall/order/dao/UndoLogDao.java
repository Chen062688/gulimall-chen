package com.atguigu.gulimall.order.dao;

import com.atguigu.gulimall.order.entity.UndoLogEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 
 * 
 * @author chen
 * @email sunlightcs@gmail.com
 * @date 2022-09-20 13:21:12
 */
@Mapper
public interface UndoLogDao extends BaseMapper<UndoLogEntity> {
	
}
