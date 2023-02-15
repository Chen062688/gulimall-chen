package com.atguigu.gulimall.seckill.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.to.mq.SeckillOrderTo;
import com.atguigu.common.utils.R;
import com.atguigu.common.vo.MemberRespVo;
import com.atguigu.gulimall.seckill.fegin.CouponFeginService;
import com.atguigu.gulimall.seckill.fegin.ProductFeginService;
import com.atguigu.gulimall.seckill.interceptor.LoginUserInterceptor;
import com.atguigu.gulimall.seckill.service.SeckillService;
import com.atguigu.gulimall.seckill.to.SeckillSkuRedisTo;
import com.atguigu.gulimall.seckill.vo.SeckillSessionsWithSkus;
import com.atguigu.gulimall.seckill.vo.SeckillSkuVo;
import com.atguigu.gulimall.seckill.vo.SkuInfoVo;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.bouncycastle.asn1.x509.Time;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SeckillServiceImpl implements SeckillService {

    @Autowired
    CouponFeginService couponFeginService;

    @Autowired
    RedisTemplate redisTemplate;

    @Autowired
    ProductFeginService productFeginService;

    @Autowired
    RedissonClient redissonClient;

    @Autowired
    RabbitTemplate rabbitTemplate;
    private final String SESSIONS_CACHE_PREFIX = "seckill:sessions:";
    private final String SECKILL_CHARE_PREFIX = "seckill:skus:";
    private final String SKU_STOCK_SEMAPHORE = "seckill:stock:"; //加商品随机码

    @Override
    public void uploadSeckillSkuLatest3Days() {
        //1.去扫描最近三天需要参与秒杀的活动
        R daySession = couponFeginService.getLates3DaySession();
        if (daySession.getCode() == 0) {
            //上架商品信息
            List<SeckillSessionsWithSkus> data = daySession.getData(new TypeReference<List<SeckillSessionsWithSkus>>() {
            });
            //1.缓存到redis中
            saveSessionInfos(data);
            //1.缓存活动关联信息
            saveSessionSkuInfos(data);
        }
    }

    /**
     * 获取返回当前时间可以参与的秒杀商品信息
     *
     * @return
     */
    @Override
    public List<SeckillSkuRedisTo> getCurrentSeckillSkus() {
        //1.确定当前时间属于哪个秒杀场次
        long time = new Date().getTime();

        Set<String> keys = redisTemplate.keys(SESSIONS_CACHE_PREFIX + "*");
        for (String key : keys) {
            String replace = key.replace(SESSIONS_CACHE_PREFIX, "");
            String[] split = replace.split("_");
            Long startTime = Long.parseLong(split[0]);
            Long endTime = Long.parseLong(split[1]);
            if (time >= startTime && time <= endTime) {
                //2.获取这个秒杀场次需要的所有商品信息
                List<String> range = redisTemplate.opsForList().range(key, -100, 100);
                BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SECKILL_CHARE_PREFIX);
                List<String> list = hashOps.multiGet(range);
                if (list != null) {
                    List<SeckillSkuRedisTo> collect = list.stream().map(item -> {
                        SeckillSkuRedisTo redisTo = JSON.parseObject((String) item, SeckillSkuRedisTo.class);
                        //redisTo.setRandomCode(null); 当前秒杀开始了就需要随机码
                        return redisTo;
                    }).collect(Collectors.toList());
                    return collect;
                }
                break;
            }
        }


        return null;
    }

    @Override
    public SeckillSkuRedisTo skuSeckillInfo(Long skuId) {

        //1.找到所有需要参与秒杀的商品的key
        BoundHashOperations<String, String, String> ops = redisTemplate.boundHashOps(SECKILL_CHARE_PREFIX);

        Set<String> keys = ops.keys();
        if (keys != null && keys.size() > 0) {
            String reg = "\\d_" + skuId;
            for (String key : keys) {
                //3_50
                if (Pattern.matches(reg, key)) {
                    String json = ops.get(key);
                    SeckillSkuRedisTo redisTo = JSON.parseObject(json, SeckillSkuRedisTo.class);
                    long current = new Date().getTime();
                    //随机码
                    Long startTime = redisTo.getStartTime();
                    Long endTime = redisTo.getEndTime();
                    if (current >= startTime && current <= endTime) {

                    } else {
                        redisTo.setRandomCode(null);
                    }

                    return redisTo;
                }

            }
        }
        return null;
    }

    //7_1
    // TODO 上架商品秒杀的时候,每一个数据都有过期时间
    @Override
    public String kull(String killId, String key, Integer num) {
        long s1 = System.currentTimeMillis();
        MemberRespVo respVo = LoginUserInterceptor.localUser.get();
        //1.获取当前商品的秒杀信息
        BoundHashOperations<String, String, String> ops = redisTemplate.boundHashOps(SECKILL_CHARE_PREFIX);

        String json = ops.get(killId);
        if (StringUtils.isEmpty(json)) {
            return null;
        } else {
            SeckillSkuRedisTo redisTo = JSON.parseObject(json, SeckillSkuRedisTo.class);
            //校验合法性
            Long startTime = redisTo.getStartTime();
            Long endTime = redisTo.getEndTime();
            long time = new Date().getTime();
            long ttl = endTime - time;
            //1.校验时间合法性
            if (time >= startTime && time <= endTime) {
                //2.校验随机码和我们的商品id是否正确
                String randomCode = redisTo.getRandomCode();
                String skuId = redisTo.getPromotionSessionId() + "_" + redisTo.getSkuId();
                if (randomCode.equals(key) && killId.equals(skuId)) {
                    //3.验证购物的数量是否合理

                    if (num <= redisTo.getSeckillLimit()) {
                        //4.验证这个人是否已经买过了 幂等性处理; 如果只要秒杀成功！就去redis里面去占位。userId_skuId_SessionId
                        //setNX
                        String redisKey = respVo.getId() + "_" + skuId;
                        //自动过期
                        Boolean aBoolean = redisTemplate.opsForValue().setIfAbsent(redisKey, num.toString(), ttl, TimeUnit.MILLISECONDS);
                        if (aBoolean) {
                            //占位成功说明从来没有买过！
                            RSemaphore semaphore = redissonClient.getSemaphore(SKU_STOCK_SEMAPHORE + randomCode);

                      
                            boolean tryAcquire = semaphore.tryAcquire(num);
                            if (tryAcquire) {
                                //秒杀成功!
                                //快速下单。 发送MQ消息 10ms
                                String timeId = IdWorker.getTimeId();
                                SeckillOrderTo orderTo = new SeckillOrderTo();
                                orderTo.setOrderSn(timeId);
                                orderTo.setMemberId(respVo.getId());
                                orderTo.setNum(num);
                                orderTo.setPromotionSessionId(redisTo.getPromotionSessionId());
                                orderTo.setSkuId(redisTo.getSkuId());
                                orderTo.setSeckillPrice(redisTo.getSeckillPrice());
                                rabbitTemplate.convertAndSend("order-event-exchange", "order.seckill.order", orderTo);
                                long s2 = System.currentTimeMillis();
                              //  log.info("耗时...",(s2-s1));
                                System.err.println("耗时....."+(s2-s1));
                                return timeId;
                            }
                            return null;
                        } else {
                            //说明这个人已经买了
                            return null;
                        }
        
                    }
                } else {
                    return null;
                }
            } else {
                return null;
            }
        }
        return null;
    }

    /**
     * 保存当前活动的问题
     *
     * @param sessions
     */
    private void saveSessionInfos(List<SeckillSessionsWithSkus> sessions) {
        if (sessions != null) {
            sessions.stream().forEach(session -> {
                Long startTime = session.getStartTime().getTime();
                Long endTime = session.getEndTime().getTime();
                String key = SESSIONS_CACHE_PREFIX + startTime + "_" + endTime;
                Boolean hasKey = redisTemplate.hasKey(key);

                if (!hasKey) {
                    List<String> collect = session.getRelationSkus().stream().map(item -> item.getPromotionSessionId() + "_" + item.getSkuId().toString()).collect(Collectors.toList());
                    //缓存活动信息
                    redisTemplate.opsForList().leftPushAll(key, collect);
                }
            });
        } else {

        }
    }

    //保持活动的商品信息
    private void saveSessionSkuInfos(List<SeckillSessionsWithSkus> sessions) {
        if (sessions != null) {
            sessions.stream().forEach(session -> {
                //准备Hash操作
                BoundHashOperations<String, Object, Object> ops = redisTemplate.boundHashOps(SECKILL_CHARE_PREFIX);
                session.getRelationSkus().stream().forEach(seckillSkuVo -> {
                    //4.商品的随机码 
                    String token = UUID.randomUUID().toString().replace("-", "");
                    if (!ops.hasKey(seckillSkuVo.getPromotionSessionId() + "_" + seckillSkuVo.getSkuId().toString())) {
                        //缓存商品
                        SeckillSkuRedisTo redisTo = new SeckillSkuRedisTo();
                        //1.sku的基本数据
                        R info = productFeginService.SkuInfo(seckillSkuVo.getSkuId());
                        if (info.getCode() == 0) {
                            SkuInfoVo skuInfo = info.getData("skuInfo", new TypeReference<SkuInfoVo>() {
                            });
                            redisTo.setSkuInfo(skuInfo);
                        }
                        //2.sku的秒杀信息
                        BeanUtils.copyProperties(seckillSkuVo, redisTo);

                        //3.设置上当前商品的秒杀时间信息
                        redisTo.setStartTime(session.getStartTime().getTime());
                        redisTo.setEndTime(session.getEndTime().getTime());

                        redisTo.setRandomCode(token);
                        //如果当前这个场次的商品的库存信息已经上架就不需要上架了
                        ops.put(seckillSkuVo.getPromotionSessionId().toString() + "_" + seckillSkuVo.getSkuId().toString(), JSON.toJSONString(redisTo));
                        //5.使用库存作为分布式信号量 限流;
                        RSemaphore semaphore = redissonClient.getSemaphore(SKU_STOCK_SEMAPHORE + token);
                        //商品可以秒杀的数量作为信号量
                        semaphore.trySetPermits(seckillSkuVo.getSeckillCount());
                    }
                });
            });
        }
    }
}
