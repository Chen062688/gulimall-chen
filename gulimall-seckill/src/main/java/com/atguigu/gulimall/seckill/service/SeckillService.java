package com.atguigu.gulimall.seckill.service;

import com.atguigu.gulimall.seckill.to.SeckillSkuRedisTo;

import java.util.List;

public interface  SeckillService {
    void uploadSeckillSkuLatest3Days();

    List<SeckillSkuRedisTo> getCurrentSeckillSkus();

    /**
     * 获取某一个sku商品的方法
     * @param skuId
     * @return
     */
    SeckillSkuRedisTo skuSeckillInfo(Long skuId);

    /**
     * 
     * @param killId
     * @param key
     * @param num
     * @return
     */
    String kull(String killId, String key, Integer num);
}
